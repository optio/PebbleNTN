"""Where the emulator runs. From WSL the heavy emulator must run on Windows (nested virtualisation
inside WSL crashed it, #55), so the Windows SDK is driven from here over adb.exe; on native Linux the
local SDK (ANDROID_HOME, with KVM) is used.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import tempfile
import time
import urllib.request
import zipfile
from pathlib import Path

REPOSITORY_XML = "https://dl.google.com/android/repository/repository2-3.xml"


class Host:
    """An Android SDK plus the emulator and adb it provides."""

    name = "host"
    exe = ""  # ".exe" on Windows

    def __init__(self, sdk: Path):
        self.sdk = sdk

    # --- paths ---------------------------------------------------------------------------------
    @property
    def adb_path(self) -> Path:
        return self.sdk / "platform-tools" / f"adb{self.exe}"

    @property
    def emulator_path(self) -> Path:
        return self.sdk / "emulator" / f"emulator{self.exe}"

    def to_host_path(self, path: Path) -> str:
        """A local file's path as the SDK tools see it."""
        return str(path)

    def scratch_dir(self) -> Path:
        d = Path(tempfile.gettempdir()) / "pebblentn-route-capture"
        d.mkdir(parents=True, exist_ok=True)
        return d

    # --- adb -----------------------------------------------------------------------------------
    def adb(self, *args: str, timeout: float = 60, check: bool = True) -> str:
        r = subprocess.run([str(self.adb_path), *args], capture_output=True, text=True, timeout=timeout)
        if check and r.returncode != 0:
            raise RuntimeError(f"adb {' '.join(args)} failed: {r.stderr.strip() or r.stdout.strip()}")
        return r.stdout.replace("\r\n", "\n")

    def shell(self, command: str, timeout: float = 60, check: bool = True) -> str:
        return self.adb("shell", command, timeout=timeout, check=check)

    def booted(self) -> bool:
        try:
            return self.shell("getprop sys.boot_completed", timeout=10, check=False).strip() == "1"
        except subprocess.TimeoutExpired:
            return False

    def wait_boot(self, timeout: float = 600) -> None:
        deadline = time.time() + timeout
        while time.time() < deadline:
            if self.booted():
                return
            time.sleep(5)
        raise TimeoutError("emulator did not finish booting")

    LOCALE_DEX = Path(__file__).resolve().parents[1] / "device" / "routecap-locale.dex"

    def system_locale(self) -> str:
        """The current system language as a tag, e.g. 'pl-PL', from `am get-config`."""
        return locale_from_config(self.shell("am get-config", check=False))

    def set_system_locale(self, tag: str) -> str:
        """Set the Android system language without root (device/SetSystemLocale.java); returns the
        language the device reports afterwards."""
        self.adb("push", self.to_host_path(self._staged(self.LOCALE_DEX)), "/data/local/tmp/routecap-locale.dex")
        self.shell("appops set com.android.shell WRITE_SETTINGS allow", check=False)
        self.shell(f"CLASSPATH=/data/local/tmp/routecap-locale.dex app_process / SetSystemLocale {tag}", check=False, timeout=60)
        time.sleep(4)  # apps restart on a language change
        return self.system_locale()

    def _staged(self, path: Path) -> Path:
        """A copy of a repo file where the SDK tools can read it."""
        target = self.scratch_dir() / path.name
        if not target.exists() or target.read_bytes() != path.read_bytes():
            target.write_bytes(path.read_bytes())
        return target

    def stop_emulator(self) -> None:
        self.adb("emu", "kill", check=False, timeout=30)

    # --- SDK setup (overridden for Windows) ----------------------------------------------------
    def sdkmanager(self, *args: str) -> None:
        tool = self.sdk / "cmdline-tools" / "latest" / "bin" / "sdkmanager"
        subprocess.run([str(tool), *args], input="y\n" * 20, text=True, check=True)

    def avdmanager(self, *args: str) -> None:
        tool = self.sdk / "cmdline-tools" / "latest" / "bin" / "avdmanager"
        subprocess.run([str(tool), *args], input="no\n", text=True, check=True)

    def avd_dir(self) -> Path:
        return Path.home() / ".android" / "avd"

    def start_emulator(self, avd: str) -> None:
        subprocess.Popen([str(self.emulator_path), "-avd", avd, "-no-boot-anim", "-no-snapshot-load"],
                         stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)

    def cmdline_tools_platform(self) -> str:
        return "linux"


class LinuxHost(Host):
    name = "linux"


class WindowsFromWslHost(Host):
    """The Windows SDK (%LOCALAPPDATA%\\Android\\Sdk) driven from WSL."""

    name = "windows (from WSL)"
    exe = ".exe"

    def __init__(self):
        local = _cmd("echo %LOCALAPPDATA%")
        super().__init__(Path(_wslpath(local + "\\Android\\Sdk")))
        self._win_sdk = local + "\\Android\\Sdk"
        self._win_temp = _cmd("echo %TEMP%")

    def to_host_path(self, path: Path) -> str:
        return subprocess.run(["wslpath", "-w", str(path)], capture_output=True, text=True, check=True).stdout.strip()

    def scratch_dir(self) -> Path:
        d = Path(_wslpath(self._win_temp)) / "pebblentn-route-capture"
        d.mkdir(parents=True, exist_ok=True)
        return d

    def avd_dir(self) -> Path:
        return Path(_wslpath(_cmd("echo %USERPROFILE%") + "\\.android\\avd"))

    def _java_home(self) -> str:
        java = _cmd("where java").splitlines()[0]
        return java.rsplit("\\bin\\", 1)[0]

    def _batch(self, body: str) -> None:
        # Windows' cmd mangles quoting and semicolons when called with arguments from WSL, so the
        # SDK tools run from a batch file.
        bat = self.scratch_dir() / "sdk-command.bat"
        bat.write_text(f'@echo off\r\nset "JAVA_HOME={self._java_home()}"\r\n{body}\r\n', encoding="utf-8")
        subprocess.run(["cmd.exe", "/c", self.to_host_path(bat)], cwd="/mnt/c", check=True)

    def sdkmanager(self, *args: str) -> None:
        quoted = " ".join(f'"{a}"' for a in args)
        self._batch(f'(for /l %%i in (1,1,20) do @echo y) | "{self._win_sdk}\\cmdline-tools\\latest\\bin\\sdkmanager.bat" {quoted}')

    def avdmanager(self, *args: str) -> None:
        quoted = " ".join(f'"{a}"' for a in args)
        self._batch(f'echo no | "{self._win_sdk}\\cmdline-tools\\latest\\bin\\avdmanager.bat" {quoted}')

    def start_emulator(self, avd: str) -> None:
        emulator = f"{self._win_sdk}\\emulator\\emulator.exe"
        subprocess.run(["powershell.exe", "-NoProfile", "-Command",
                        f"Start-Process -FilePath '{emulator}' -ArgumentList '-avd','{avd}','-no-boot-anim','-no-snapshot-load' "
                        f"-WorkingDirectory '{self._win_sdk}\\emulator'"], cwd="/mnt/c", check=True)
        # Windows' adb server must be up for `adb.exe wait-for-device`.
        self.adb("start-server", check=False)

    def cmdline_tools_platform(self) -> str:
        return "win"


def locale_from_config(config: str) -> str:
    """'pl-PL' from an `am get-config` line such as 'config: mcc310-mnc260-pl-rPL-ldltr-...'."""
    m = re.search(r"config: .*?-([a-z]{2,3})-r([A-Z]{2})-", config)
    if m:
        return f"{m.group(1)}-{m.group(2)}"
    m = re.search(r"config: (?:mcc\d+-mnc\d+-)?([a-z]{2,3})-", config)
    return m.group(1) if m else ""


def _cmd(command: str) -> str:
    return subprocess.run(["cmd.exe", "/c", command], capture_output=True, text=True, cwd="/mnt/c").stdout.strip()


def _wslpath(windows_path: str) -> str:
    return subprocess.run(["wslpath", "-u", windows_path], capture_output=True, text=True, check=True).stdout.strip()


def detect() -> Host:
    """Windows SDK when running in WSL, otherwise the local SDK."""
    if "microsoft" in Path("/proc/version").read_text().lower():
        return WindowsFromWslHost()
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or str(Path.home() / "Android" / "Sdk")
    return LinuxHost(Path(sdk))


def ensure_cmdline_tools(host: Host) -> None:
    """Install the SDK command-line tools (sdkmanager, avdmanager) if the SDK lacks them."""
    if (host.sdk / "cmdline-tools" / "latest" / "bin").exists():
        return
    xml = urllib.request.urlopen(REPOSITORY_XML, timeout=60).read().decode()
    block = re.search(r'<remotePackage path="cmdline-tools;latest">.*?</remotePackage>', xml, re.S).group(0)
    name = re.search(rf"<url>(commandlinetools-{host.cmdline_tools_platform()}-[^<]+)</url>", block).group(1)
    zip_path = host.scratch_dir() / name
    urllib.request.urlretrieve(f"https://dl.google.com/android/repository/{name}", zip_path)
    target = host.sdk / "cmdline-tools"
    with zipfile.ZipFile(zip_path) as z:
        z.extractall(host.scratch_dir() / "clt")
    target.mkdir(parents=True, exist_ok=True)
    shutil.move(str(host.scratch_dir() / "clt" / "cmdline-tools"), str(target / "latest"))


def installed(host: Host, package: str) -> bool:
    """Whether an SDK package ("platform-tools", "emulator", "system-images;...") is installed."""
    return (host.sdk / Path(*package.split(";")) / "package.xml").exists()


def configure_avd(config_ini: str, ram_mb: int, cores: int, data_gb: int) -> str:
    """Play Store on, enough RAM, cores and data for map apps; GPS on."""
    wanted = {"PlayStore.enabled": "yes", "hw.ramSize": f"{ram_mb}M", "hw.cpu.ncore": str(cores),
              "disk.dataPartition.size": f"{data_gb}G", "hw.gps": "yes", "hw.gpu.enabled": "yes", "hw.gpu.mode": "auto"}
    lines = [l for l in config_ini.splitlines() if l and not l.startswith("disk.dataPartition.path=")]
    seen = set()
    out = []
    for line in lines:
        k = line.split("=", 1)[0]
        if k in wanted:
            out.append(f"{k}={wanted[k]}")
            seen.add(k)
        else:
            out.append(line)
    out += [f"{k}={v}" for k, v in wanted.items() if k not in seen]
    return "\n".join(out) + "\n"
