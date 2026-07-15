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
        // PebbleKit Android 2 (io.rebble.pebblekit2:client) is published on JitPack, not Maven
        // Central. Without this the io.rebble.pebblekit2.* classes are absent from the compile
        // classpath (every import unresolved).
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PebbleNTN"
include(":app")
include(":fixture-publisher")
