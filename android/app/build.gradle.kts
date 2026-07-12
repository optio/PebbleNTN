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
        include("*.json")
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

android {
    namespace = "com.pebblentn.app"
    // Compile SDK: baseline 36 per target baseline (README). Use the latest stable installed SDK.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pebblentn.app"
        minSdk = 31
        // Target SDK baseline 35; release automation verifies the Play-required target at release time.
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
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
