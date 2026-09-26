package app.funput.build.tokens

import groovy.json.JsonException
import groovy.json.JsonSlurper

/** Everything wrong with a token file, each entry prefixed by the token path it concerns. */
class DesignTokenException(val problems: List<String>) :
    IllegalArgumentException(problems.joinToString(separator = "\n", prefix = "Invalid design tokens:\n"))

/**
 * Reads the token file into [DesignTokens].
 *
 * It reports every structural problem it finds rather than stopping at the first, so one build
 * tells a contributor everything to fix. Value rules (ranges, contrast) live in [DesignTokenRules];
 * this only checks that each value has the right shape.
 */
object DesignTokenParser {
    /** Parses [json], or throws [DesignTokenException] listing every problem. */
    fun parse(json: String): DesignTokens {
        val root = try {
            JsonSlurper().parseText(json)
        } catch (error: JsonException) {
            throw DesignTokenException(listOf("<file>: not valid JSON (${error.message})"))
        }
        val problems = mutableListOf<String>()
        val tokens = Reader(problems).read(root)
        if (problems.isNotEmpty() || tokens == null) throw DesignTokenException(problems)
        return tokens
    }

    private class Reader(private val problems: MutableList<String>) {
        fun read(root: Any?): DesignTokens? {
            val top = root.asObject("<root>") ?: return null
            val version = (top["schemaVersion"] as? Number)?.toInt()
            if (version == null) problems += "schemaVersion: missing or not a number"
            return DesignTokens(
                schemaVersion = version ?: 0,
                colors = group(top, "color") { path, value -> color(path, value) },
                numbers = DesignTokens.NumberGroups.associateWith { name ->
                    group(top, name) { path, value -> number(path, value) }
                },
                typography = group(top, "typography") { path, value -> textStyle(path, value) },
                motion = group(top, "motion") { path, value -> motion(path, value) },
            )
        }

        private fun <T : Any> group(top: Map<*, *>, name: String, read: (String, Any?) -> T?): Map<String, T> {
            val entries = top[name].asObject(name) ?: return emptyMap()
            return entries.entries.mapNotNull { (key, value) ->
                read("$name.$key", value)?.let { key.toString() to it }
            }.toMap()
        }

        private fun color(path: String, value: Any?): ColorPair? {
            val modes = value.asObject(path) ?: return null
            val light = hex("$path.light", modes["light"])
            val dark = hex("$path.dark", modes["dark"])
            return if (light != null && dark != null) ColorPair(light, dark) else null
        }

        private fun hex(path: String, value: Any?): RgbaColor? {
            val parsed = (value as? String)?.let(RgbaColor::parse)
            if (parsed == null) problems += "$path: expected #RRGGBB or #RRGGBBAA, got ${value ?: "nothing"}"
            return parsed
        }

        private fun number(path: String, value: Any?): Double? {
            val parsed = (value as? Number)?.toDouble()
            if (parsed == null) problems += "$path: expected a number, got ${value ?: "nothing"}"
            return parsed
        }

        private fun textStyle(path: String, value: Any?): TextStyleToken? {
            val style = value.asObject(path) ?: return null
            val size = number("$path.size", style["size"])
            val lineHeight = number("$path.lineHeight", style["lineHeight"])
            val weight = number("$path.weight", style["weight"])?.toInt()
            if (size == null || lineHeight == null || weight == null) return null
            return TextStyleToken(size, lineHeight, weight)
        }

        private fun motion(path: String, value: Any?): MotionToken? {
            val spec = value.asObject(path) ?: return null
            val curveName = spec["curve"] as? String
            val curve = curveName?.let(MotionCurve::fromJson)
            if (curve == null) {
                val known = MotionCurve.entries.joinToString { it.jsonName }
                problems += "$path.curve: expected one of $known, got ${curveName ?: "nothing"}"
            }
            val duration = number("$path.durationMs", spec["durationMs"])?.toInt()
            val bounce = spec["bounce"]?.let { number("$path.bounce", it) }
            if (curve == MotionCurve.SPRING && bounce == null && "bounce" !in spec) {
                problems += "$path.bounce: a spring needs a bounce"
            }
            if (curve == null || duration == null) return null
            return MotionToken(curve, duration, bounce)
        }

        private fun Any?.asObject(path: String): Map<*, *>? {
            if (this !is Map<*, *>) problems += "$path: expected an object"
            return this as? Map<*, *>
        }
    }
}
