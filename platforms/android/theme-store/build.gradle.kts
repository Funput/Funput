plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.funput.funput.theme.store"
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
}

dependencies {
    implementation(project(":theme-runtime"))
    testImplementation(libs.junit)
}
