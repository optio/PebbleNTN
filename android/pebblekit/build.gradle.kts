plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// Vendored PebbleKit Android 2 (io.rebble.pebblekit2) — see README.md. The upstream ships this as
// four small Android library modules (common-api, common, client-api, client); they are flattened
// into this single module so the app builds entirely from source under its own toolchain, with no
// JitPack / prebuilt-artifact dependency (required for F-Droid).
android {
    namespace = "io.rebble.pebblekit2"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
    }

    buildFeatures {
        aidl = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.kermit)
}

// Kotlin 2.3 removed the String-based kotlinOptions.jvmTarget; use the compilerOptions DSL.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}
