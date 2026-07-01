plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.funput.funput.keyboard"
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

dependencies {
    implementation(project(":theme-runtime"))
    implementation(libs.androidx.core.ktx)
}
