import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

abstract class GenerateEmojiCatalog : DefaultTask() {
    @get:InputFile abstract val sourceFile: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val target = outputDirectory.file("EmojiCatalog.json").get().asFile
        target.parentFile.mkdirs()
        sourceFile.get().asFile.copyTo(target, overwrite = true)
        check(sourceFile.get().asFile.readBytes().contentEquals(target.readBytes())) {
            "Android and iOS emoji catalogs differ"
        }
    }
}

val canonicalEmojiCatalog =
    layout.projectDirectory.file("../../ios/Packages/FunputKit/Sources/KeyboardRenderer/Resources/EmojiCatalog.json")
val generatedEmojiAssets = layout.buildDirectory.dir("generated/funputEmojiAssets")
val syncEmojiCatalog by tasks.registering(GenerateEmojiCatalog::class) {
    sourceFile.set(canonicalEmojiCatalog)
    outputDirectory.set(generatedEmojiAssets)
}
val verifyEmojiCatalogParity by tasks.registering {
    dependsOn(syncEmojiCatalog)
}

android {
    namespace = "app.funput.funput.keyboard.ui"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }

}

androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(
        syncEmojiCatalog,
        GenerateEmojiCatalog::outputDirectory,
    )
    tasks.named("preBuild").configure { dependsOn(verifyEmojiCatalogParity) }
}

dependencies {
    api(project(":keyboard-renderer"))
    api(project(":theme-runtime"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.savedstate)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
