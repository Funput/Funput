package app.funput.build.tokens

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when the shared design-token file is malformed or breaks a [DesignTokenRules]
 * rule, listing every problem at once.
 *
 * The stamp file exists only so Gradle can skip the task while the token file is unchanged.
 */
@CacheableTask
abstract class ValidateDesignTokensTask : DefaultTask() {
    /** The shared token file, `design/tokens/app.tokens.json` at the repository root. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tokenFile: RegularFileProperty

    /** Written after a successful check; its content is not meant to be read. */
    @get:OutputFile
    abstract val stampFile: RegularFileProperty

    /** Parses and checks [tokenFile]. */
    @TaskAction
    fun validate() {
        val source = tokenFile.get().asFile
        val problems = try {
            DesignTokenRules.violations(DesignTokenParser.parse(source.readText()))
        } catch (error: DesignTokenException) {
            error.problems
        }
        if (problems.isNotEmpty()) {
            throw GradleException(
                problems.joinToString(separator = "\n  ", prefix = "${source.path} is invalid:\n  "),
            )
        }
        stampFile.get().asFile.writeText("valid\n")
    }
}
