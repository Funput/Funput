plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// Shared JVM UI-test support: Robolectric renders Compose, Roborazzi captures it. Modules pull
// this in with `testImplementation`, so nothing here ever reaches an APK.
android {
    namespace = "app.funput.funput.uitesting"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.test.junit4)
    // Registers the empty ComponentActivity that createComposeRule() launches.
    api(libs.androidx.compose.ui.test.manifest)
    api(libs.junit)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.roborazzi.compose)
}
