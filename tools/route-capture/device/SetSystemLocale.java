/*
 * Sets the Android system language from the shell, without root (route-capture harness, #55).
 *
 * Run with: CLASSPATH=/data/local/tmp/routecap-locale.dex app_process / SetSystemLocale pl-PL
 *
 * app_process runs as the shell user, which holds CHANGE_CONFIGURATION and WRITE_SETTINGS (the harness
 * also allows the WRITE_SETTINGS app-op for com.android.shell). It updates the persistent
 * configuration the way Settings > System > Languages does, attributed to com.android.shell: going
 * through LocalePicker fails here, because an app_process has no package name to attribute it to.
 * Reflection only, so it builds with a plain JDK.
 */
public final class SetSystemLocale {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: SetSystemLocale <language-tag[,language-tag...]>");
            System.exit(2);
        }
        Class<?> localeList = Class.forName("android.os.LocaleList");
        Object locales = localeList.getMethod("forLanguageTags", String.class).invoke(null, args[0]);

        Object am = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
        Class<?> iam = Class.forName("android.app.IActivityManager");
        Class<?> configurationClass = Class.forName("android.content.res.Configuration");
        Object config = iam.getMethod("getConfiguration").invoke(am);
        configurationClass.getMethod("setLocales", localeList).invoke(config, locales);
        // userSetLocale: the change came from the user, like picking a language in Settings.
        configurationClass.getField("userSetLocale").setBoolean(config, true);
        iam.getMethod("updatePersistentConfigurationWithAttribution", configurationClass, String.class, String.class)
            .invoke(am, config, "com.android.shell", null);
        System.out.println("system locales: " + localeList.getMethod("toLanguageTags").invoke(locales));
    }
}
