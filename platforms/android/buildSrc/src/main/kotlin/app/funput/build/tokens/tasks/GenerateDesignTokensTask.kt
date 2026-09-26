package app.funput.build.tokens.tasks

import app.funput.build.tokens.codegen.KotlinTokenEmitter
import app.funput.build.tokens.validation.DesignTokenException
import app.funput.build.tokens.validation.DesignTokenParser
import app.funput.build.tokens.validation.DesignTokenRules
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Validates the shared token file and, only when it passes every rule, writes `FunputTokens.kt`
 * into [outputDirectory] for the module to compile. A bad token therefore stops the build before
 * any code exists that could use it.
 */
@CacheableTask
abstract class GenerateDesignTokensTask : DefaultTask() {
    /** The shared token file, `design/tokens/app.tokens.json` at the repository root. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tokenFile: RegularFileProperty

    /** Package of the generated declarations. */
    @get:Input
    abstract val packageName: Property<String>

    /** Source root the generated file is written under, by package path. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /** Parses, validates and emits. */
    @TaskAction
    fun generate() {
        val source = tokenFile.get().asFile
        val tokens = try {
            DesignTokenParser.parse(source.readText())
        } catch (error: DesignTokenException) {
            throw failure(source.path, error.problems)
        }
        DesignTokenRules.violations(tokens).takeIf { it.isNotEmpty() }?.let { throw failure(source.path, it) }

        val root = outputDirectory.get().asFile.apply { deleteRecursively() }
        val target = root.resolve(packageName.get().replace('.', '/')).apply { mkdirs() }
        target.resolve("FunputTokens.kt").writeText(KotlinTokenEmitter(packageName.get()).emit(tokens))
    }

    private fun failure(path: String, problems: List<String>) =
        GradleException(problems.joinToString(separator = "\n  ", prefix = "$path is invalid:\n  "))
}
