package app.funput.build

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/** Cross-compiles the Rust JNI library into one Android ABI source directory. */
abstract class BuildRustJniTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:Input
    abstract val rustTarget: Property<String>

    @get:Input
    abstract val abi: Property<String>

    @get:Input
    abstract val cargoProfile: Property<String>

    @get:Input
    abstract val ndkVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildScript: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rustSources: ConfigurableFileCollection

    @get:Internal
    abstract val workspaceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun build() {
        fileSystemOperations.delete { delete(outputDirectory) }
        val abiOutput = outputDirectory.get().dir(abi.get())
        execOperations.exec {
            workingDir(workspaceDirectory)
            commandLine(
                "bash",
                buildScript.get().asFile,
                rustTarget.get(),
                abi.get(),
                cargoProfile.get(),
                abiOutput.asFile,
                ndkVersion.get(),
            )
        }.assertNormalExitValue()
    }
}
