pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PebbleNTN"
include(":app")
include(":fixture-publisher")
// PebbleKit Android 2 vendored from source (see android/pebblekit/README.md) instead of JitPack, so
// the whole app builds from source with no prebuilt-artifact dependency (required for F-Droid).
include(":pebblekit")
