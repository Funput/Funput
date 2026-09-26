plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Throwaway spike (never merged): compares glass techniques on a real device. Its own
// applicationId, so it installs beside Funput instead of replacing it.
android {
    namespace = "app.funput.funput.glassspike"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.funput.funput.glassspike"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "spike"
    }

    buildTypes {
        // Measured build: optimized like a release, debug-signed so it installs.
        create("profiling") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += "release"
        }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("dev.chrisbanes.haze:haze:2.0.0")
    implementation("dev.chrisbanes.haze:haze-blur:2.0.0")
}
