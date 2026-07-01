plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.funput.funput.theme"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}
