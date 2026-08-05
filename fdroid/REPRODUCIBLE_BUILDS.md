# Reproducible builds on F-Droid — setup

Enabling **reproducible builds** makes F-Droid rebuild the app from source, verify it
matches the release-signed APK you publish (byte-for-byte, ignoring the signature), and
then distribute **your** signed APK. Users can then move between the GitHub/Rebble build
and the F-Droid build without uninstalling, and get cryptographic proof the F-Droid APK
came from this source.

> **This choice is permanent.** If F-Droid ships the app signed with *its* key first, it
> can never later switch to your signature (that would change the app's signing key,
> which Android refuses to update across). So decide before the "New App" merge request.

## What's already done (code side)

- **`dependenciesInfo { includeInApk = false; includeInBundle = false }`** — removes the
  Google-signed dependency-metadata blob AGP otherwise embeds, which a from-source
  rebuild cannot recreate. This was the one hard blocker.
- The build is otherwise deterministic: every dependency and the whole toolchain
  (Gradle, AGP, build-tools) are version-pinned, and there are no build timestamps or
  git-hash injection.
- The release workflow already produces a signed release APK when the signing secrets
  are present (`.github/workflows/release.yml`).

So the build is reproducible-ready. What remains needs **your signing key**, which only
you can create and hold — I must never see it.

## What you need to do

### 1. Create a release keystore

Keep this file and its passwords safe and backed up. **If you lose it, you can never
publish another update under this signature** — on any channel.

```
keytool -genkeypair -v \
  -keystore pebblentn-release.jks \
  -alias pebblentn \
  -keyalg RSA -keysize 4096 -validity 10000
```

Pick a strong store password and key password and answer the identity prompts.

### 2. Add four GitHub Actions secrets

In the repo: **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | Value |
|--------|-------|
| `UPLOAD_KEYSTORE_BASE64` | output of `base64 -w0 pebblentn-release.jks` (the whole file) |
| `KEYSTORE_PASSWORD` | the store password from step 1 |
| `KEY_ALIAS` | `pebblentn` |
| `KEY_PASSWORD` | the key password from step 1 |

### 3. Cut a release-signed build

Push any commit to `main` (or run the **release** workflow via *Actions → release → Run
workflow*). CI will now publish a **release-signed** `pebble-ntn.apk` on the GitHub
release. Note the version it produces (e.g. `v0.0.22`).

### 4. Read the signing certificate's SHA-256

Download that release's `pebble-ntn.apk` and run:

```
apksigner verify --print-certs --verbose pebble-ntn.apk | grep -i "SHA-256"
```

Copy the hex digest (remove any colons, lowercase) — this is the recipe's
`AllowedAPKSigningKeys` value.

### 5. Hand off two values

Send me **the release version** (e.g. `0.0.22`) and **the SHA-256** from step 4. I'll:
- point the recipe's build at that release-signed version,
- add `Binaries:` (the URL F-Droid downloads your signed APK from) and
  `AllowedAPKSigningKeys:`,
- run a local reproducibility check (`fdroid build` compared against your signed APK)
  and fix anything that doesn't match, before you submit.

### 6. Submit with the box checked

Update the recipe in your `fdroiddata` fork with the finalized version, and in the merge
request **check "Enable reproducible builds"**.

## What the finalized recipe will look like

```yaml
Repo: https://github.com/optio/PebbleNTN.git
Binaries: https://github.com/optio/PebbleNTN/releases/download/v%v/pebble-ntn.apk

Builds:
  - versionName: 0.0.22          # the first release-signed version
    versionCode: 22
    commit: v0.0.22
    subdir: android/app
    scanignore:
      - watchapp/package.json
    gradle:
      - yes

AllowedAPKSigningKeys: <sha256 from step 4>
AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
```

F-Droid then verifies each new tag's from-source build against the `pebble-ntn.apk` you
publish for it, and ships your signed APK.
