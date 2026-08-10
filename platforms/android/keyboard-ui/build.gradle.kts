import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

abstract class GeneratePanelCatalogs : DefaultTask() {
    @get:InputFiles abstract val sourceFiles: ConfigurableFileCollection
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        sourceFiles.files.forEach { source ->
            val target = outputDirectory.file(source.name).get().asFile
            target.parentFile.mkdirs()
            source.copyTo(target, overwrite = true)
            check(source.readBytes().contentEquals(target.readBytes())) {
                "Android and iOS ${source.name} catalogs differ"
            }
        }
    }
}

val canonicalCatalogs = listOf("EmojiCatalog.json", "KaomojiCatalog.json").map {
    layout.projectDirectory.file("../../ios/Packages/FunputKit/Sources/KeyboardRenderer/Resources/$it")
}
val generatedPanelAssets = layout.buildDirectory.dir("generated/funputPanelAssets")
val syncPanelCatalogs by tasks.registering(GeneratePanelCatalogs::class) {
    sourceFiles.from(canonicalCatalogs)
    outputDirectory.set(generatedPanelAssets)
}
val verifyPanelCatalogParity by tasks.registering {
    dependsOn(syncPanelCatalogs)
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
        syncPanelCatalogs,
        GeneratePanelCatalogs::outputDirectory,
    )
    tasks.named("preBuild").configure { dependsOn(verifyPanelCatalogParity) }
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
