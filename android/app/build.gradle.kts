plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Stage bundled rule data (catalog + bundled rulesets) into build/ so it feeds both app assets and
// unit-test resources — a single source of truth for runtime and tests.
val ruleDataStageDir = layout.buildDirectory.dir("generated/rule-data")
val syncRuleData = tasks.register<Sync>("syncRuleData") {
    description = "Stage rules/catalog + rules/bundled JSON as app assets + test resources."
    into(ruleDataStageDir)
    from(rootProject.file("../rules/catalog")) {
        include("*.json")
        into("catalog")
    }
    from(rootProject.file("../rules/bundled")) {
        // Recursive: bundled rulesets live under rules/bundled/<app>/<language>.json, and Sync
        // preserves the subpath so they land at rules/bundled/<app>/<language>.json in assets.
        include("**/*.json")
        into("rules/bundled")
    }
}

// Test-only fixtures (never shipped in the APK): staged to the unit-test classpath only.
val testFixtureStageDir = layout.buildDirectory.dir("generated/test-fixtures")
val syncTestFixtures = tasks.register<Sync>("syncTestFixtures") {
    description = "Stage rules/fixtures + rules/test-cases as unit-test resources."
    into(testFixtureStageDir)
    from(rootProject.file("../rules/fixtures")) {
        include("**/*.json")
        into("fixtures")
    }
}

// Ensure rule data is staged before any task that consumes it as assets or test resources.
tasks.matching { it.name.endsWith("Assets") || it.name == "preBuild" }
    .configureEach { dependsOn(syncRuleData) }
tasks.matching { it.name.endsWith("UnitTestJavaRes") }
    .configureEach { dependsOn(syncRuleData, syncTestFixtures) }

// Export Room schemas so migration tests (M9) can diff versions.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas/room")
}

// Robolectric sandboxes are memory-heavy; give the unit-test JVM room and a single fork so the
// Android environment stays initialized across the growing Robolectric suite.
tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
    maxParallelForks = 1
}

// Release automation gate: verify the target SDK still meets the current Play submission floor
// (spec/600-security-release/BuildRelease.md). Bump PLAY_MIN_TARGET_SDK as Google raises it.
val playMinTargetSdk = 35
tasks.register("verifyReleaseTargetSdk") {
    doLast {
        val target = android.defaultConfig.targetSdk
            ?: throw GradleException("targetSdk is not set")
        if (target < playMinTargetSdk) {
            throw GradleException("targetSdk $target is below the Play submission floor $playMinTargetSdk")
        }
        println("targetSdk $target meets the Play floor ($playMinTargetSdk).")
    }
}

android {
    namespace = "com.pebblentn.app"
    // Compile SDK: baseline 36 per target baseline (README). Use the latest stable installed SDK.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pebblentn.app"
        minSdk = 31
        // Target SDK baseline 35; release automation verifies the Play-required target at release time.
        targetSdk = 35
        versionCode = 10
        versionName = "0.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing from environment (CI supplies these via a protected environment). Never
    // committed; when absent (local dev), release builds are produced unsigned.
    //
    // `takeIf { isNotBlank() }` is load-bearing: an *empty* KEYSTORE_PATH is not null, and CI sets
    // env vars to "" when a secret is missing. Without this, configuring the signing config fails
    // the whole build with "path may not be null or empty string" — even for assembleDebug.
    val releaseKeystore = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Robolectric-backed unit tests (Room, Android-dependent logic) need merged resources.
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // The bundled navigation-app catalog is authored once in rules/catalog/ (spec layout) and
    // staged into build/ so it is packaged as an app asset (catalog/navigation-apps.json) AND
    // available on the unit-test classpath — a single source of truth for runtime and tests.
    sourceSets {
        getByName("main").assets.srcDir(ruleDataStageDir)
        getByName("test").resources.srcDir(ruleDataStageDir)
        getByName("test").resources.srcDir(testFixtureStageDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    // PebbleKit Android 2 — resolved from JitPack (see android/settings.gradle.kts). JitPack builds
    // a version on first request, so a cold CI cache may need one retry while that build completes.
    implementation(libs.pebblekit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
}

// Kotlin 2.3 removed the String-based kotlinOptions.jvmTarget; the JVM target now lives in the
// top-level kotlin { compilerOptions { } } DSL.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}
