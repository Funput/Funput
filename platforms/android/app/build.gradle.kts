import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.funput.funput"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.funput.funput"
        minSdk = 26
        targetSdk = 37
        // deploy-android.yml passes both: the marketing version you type when you run
        // it, and a build number derived from the run number. Neither is committed, so
        // shipping needs no version bump in the tree and two deploys can never claim
        // the same build number. The literals below are only what a local build gets.
        versionCode = providers.gradleProperty("funput.versionCode").orNull?.toInt() ?: 27
        versionName = providers.gradleProperty("funput.versionName").orNull ?: "1.2026.70"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing is read from keystore.properties (git-ignored). When the file is
    // absent — CI, contributors, local debug builds — no release signing config exists and
    // the release artifact stays unsigned instead of failing configuration.
    signingConfigs {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            val keystoreProperties = Properties().apply {
                keystorePropertiesFile.inputStream().use { load(it) }
            }
            create("release") {
                // rootProject.file, not file: keystore.properties.example documents
                // storeFile as relative to platforms/android/, and the key really does
                // live there, but Project.file resolves against *this module*, so the
                // documented path pointed at app/upload-keystore.jks and found nothing.
                // An absolute path — what CI writes — is unaffected either way.
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":ime"))
    implementation(project(":keyboard-renderer"))
    implementation(project(":theme-runtime"))
    implementation(project(":theme-store"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

androidComponents.beforeVariants {
    (it as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true
}
