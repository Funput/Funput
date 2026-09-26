import app.funput.build.tokens.tasks.GenerateDesignTokensTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// FunputUI: the app's own design system. Tokens come from the shared design/tokens file and are
// generated into this module's sources on every build; screens depend on the components here,
// never on the tokens or on Material directly.
android {
    namespace = "app.funput.funput.ui.kit"
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
    testOptions.unitTests.isIncludeAndroidResources = true
}

val generateDesignTokens by tasks.registering(GenerateDesignTokensTask::class) {
    tokenFile.set(rootProject.layout.projectDirectory.file("../../design/tokens/app.tokens.json"))
    packageName.set("app.funput.funput.ui.kit.tokens")
    outputDirectory.set(layout.buildDirectory.dir("generated/designTokens"))
}

// Goldens are test inputs: without this, a changed or replaced golden leaves the test task
// UP-TO-DATE and verifyRoborazzi passes without comparing anything.
tasks.withType<Test>().configureEach {
    inputs.dir("src/test/screenshots").withPathSensitivity(PathSensitivity.RELATIVE).optional()
}

androidComponents.onVariants { variant ->
    variant.sources.kotlin?.addGeneratedSourceDirectory(
        generateDesignTokens,
        GenerateDesignTokensTask::outputDirectory,
    )
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.kyant.shapes)
    // Glass: blur from API 31, edge refraction from API 33. Wrapped by the glass package only.
    implementation(libs.kyant.backdrop)
    // Only for the few components FunputUI deliberately borrows (sheet, dialog, slider).
    implementation(libs.androidx.compose.material3)
    testImplementation(project(":ui-testing"))
}
