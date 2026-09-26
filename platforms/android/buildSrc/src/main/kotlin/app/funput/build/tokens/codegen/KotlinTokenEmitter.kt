package app.funput.build.tokens.codegen

import app.funput.build.tokens.model.DesignTokens
import app.funput.build.tokens.model.MotionCurve
import app.funput.build.tokens.model.RgbaColor
import java.util.Locale

/**
 * Turns validated [DesignTokens] into one Compose-ready Kotlin source file.
 *
 * Every declaration is `internal`: the tokens are raw material for the module's theme, which is
 * the only public face of them. Output is deterministic (groups and names in file order), so an
 * unchanged token file regenerates byte-identical code and Gradle can cache it.
 */
class KotlinTokenEmitter(private val packageName: String) {
    /** The complete source file for [tokens]. */
    fun emit(tokens: DesignTokens): String = buildString {
        appendLine("// Generated from design/tokens/app.tokens.json by GenerateDesignTokensTask. Do not edit.")
        appendLine("package $packageName")
        appendLine()
        Imports.forEach { appendLine("import $it") }
        appendLine()
        appendLine(Declarations)
        block("ColorTokens", tokens.colors) { (light, dark) ->
            "ColorToken(light = ${color(light)}, dark = ${color(dark)})"
        }
        tokens.numbers.forEach { (group, values) ->
            val objectName = group.replaceFirstChar { it.titlecase(Locale.ROOT) } + "Tokens"
            if (group == "opacity") block(objectName, values) { "${number(it)}f" }
            else block(objectName, values) { "${number(it)}.dp" }
        }
        block("TypeTokens", tokens.typography) { style ->
            "TypeToken(size = ${number(style.size)}.sp, lineHeight = ${number(style.lineHeight)}.sp, " +
                "weight = FontWeight(${style.weight}))"
        }
        block("MotionTokens", tokens.motion) { motion ->
            "MotionToken(curve = TokenCurve.${motion.curve.name}, durationMillis = ${motion.durationMs}, " +
                "bounce = ${number(motion.bounce ?: 0.0)}f)"
        }
    }

    private fun <T> StringBuilder.block(name: String, values: Map<String, T>, render: (T) -> String) {
        appendLine()
        appendLine("internal object $name {")
        values.forEach { (key, value) -> appendLine("    val $key = ${render(value)}") }
        appendLine("}")
    }

    private companion object {
        val Imports = listOf(
            "androidx.compose.ui.graphics.Color",
            "androidx.compose.ui.text.font.FontWeight",
            "androidx.compose.ui.unit.Dp",
            "androidx.compose.ui.unit.TextUnit",
            "androidx.compose.ui.unit.dp",
            "androidx.compose.ui.unit.sp",
        )

        val Declarations = """
            |/** One colour role in both appearances. */
            |internal class ColorToken(val light: Color, val dark: Color)
            |
            |/** A text style's size, line height and weight. */
            |internal class TypeToken(val size: TextUnit, val lineHeight: TextUnit, val weight: FontWeight)
            |
            |/** The easing families a motion token can use. */
            |internal enum class TokenCurve { ${MotionCurve.entries.joinToString { it.name }} }
            |
            |/** An animation as authored: curve, duration and (for springs) bounce. */
            |internal class MotionToken(val curve: TokenCurve, val durationMillis: Int, val bounce: Float)
            |
            |/** Keeps the Dp import used even when a token file has no dimension groups. */
            |@Suppress("unused")
            |private val ZeroDp: Dp = 0.dp
        """.trimMargin()

        /** `0xAARRGGBB`, the argument order Compose's `Color(Long)` expects. */
        fun color(value: RgbaColor): String =
            "Color(0x%02X%02X%02X%02X)".format(value.alpha, value.red, value.green, value.blue)

        /** Whole numbers without a trailing `.0`, so `22` stays `22` and `0.5` stays `0.5`. */
        fun number(value: Double): String =
            if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
    }
}
