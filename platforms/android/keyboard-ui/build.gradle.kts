plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.funput.funput.keyboard.ui"
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
    api(project(":keyboard-renderer"))
    api(project(":theme-runtime"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.emoji2.emojipicker)
    testImplementation(libs.junit)
}
