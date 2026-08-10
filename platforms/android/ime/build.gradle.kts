import app.funput.build.BuildRustJniTask

plugins {
    alias(libs.plugins.android.library)
}

val rustWorkspace = rootProject.projectDir.resolve("../..")
val rustTargets = mapOf(
    "Arm64" to ("aarch64-linux-android" to "arm64-v8a"),
    "X8664" to ("x86_64-linux-android" to "x86_64"),
)

android {
    namespace = "app.funput.funput.ime"
    ndkVersion = "29.0.14206865"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += setOf("arm64-v8a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":keyboard-ui"))
    implementation(project(":theme-store"))
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

androidComponents.beforeVariants {
    (it as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true
    (it as com.android.build.api.variant.HasAndroidTestBuilder).enableAndroidTest = true
}

androidComponents.onVariants { variant ->
    val profile = if (variant.buildType == "release") "release" else "debug"
    rustTargets.forEach { (targetName, target) ->
        val task = tasks.register<BuildRustJniTask>(
            "buildRust${targetName}${variant.name.replaceFirstChar(Char::uppercase)}",
        ) {
            group = "build"
            description = "Builds funput-jni for ${target.second} ($profile)."
            rustTarget.set(target.first)
            abi.set(target.second)
            cargoProfile.set(profile)
            ndkVersion.set(android.ndkVersion)
            buildScript.set(rootProject.layout.projectDirectory.file("scripts/build-rust-jni.sh"))
            workspaceDirectory.set(rustWorkspace)
            rustSources.from(
                rustWorkspace.resolve("Cargo.toml"),
                rustWorkspace.resolve("Cargo.lock"),
                fileTree(rustWorkspace.resolve("crates/funput-core")) { include("Cargo.toml", "src/**/*.rs") },
                fileTree(rustWorkspace.resolve("crates/funput-engine")) { include("Cargo.toml", "src/**/*.rs") },
                fileTree(rustWorkspace.resolve("crates/funput-suggestions")) {
                    include("Cargo.toml", "src/**/*.rs")
                },
                fileTree(rustWorkspace.resolve("crates/funput-jni")) { include("Cargo.toml", "src/**/*.rs") },
            )
        }
        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            task,
            BuildRustJniTask::outputDirectory,
        )
    }
}
