package app.funput.build.tokens

/**
 * A minimal token file that passes every rule, with hooks to break one part at a time. Each test
 * then changes exactly the thing it is about, so a failure points at one rule.
 */
internal object TokenFixtures {
    private val validColors = mapOf(
        "accent" to ("#D46B08" to "#FFA43F"),
        "groupedBackground" to ("#F2F2F7" to "#000000"),
        "cardBackground" to ("#FFFFFF" to "#1C1C1E"),
        "cardStroke" to ("#00000012" to "#FFFFFF12"),
        "label" to ("#000000" to "#FFFFFF"),
        "secondaryLabel" to ("#3C3C4399" to "#EBEBF599"),
        "tertiaryLabel" to ("#3C3C434D" to "#EBEBF54D"),
        "separator" to ("#3C3C434A" to "#54545899"),
        "success" to ("#34C759" to "#30D158"),
        "destructive" to ("#FF3B30" to "#FF453A"),
    )

    /** The token JSON, with [colors] overriding or (when mapped to null) removing roles. */
    fun json(
        colors: Map<String, Pair<String, String>?> = emptyMap(),
        schemaVersion: String = "1",
        radius: String = """{ "card": 22 }""",
        opacity: String = """{ "tintFill": 0.12 }""",
        typography: String = """{ "body": { "size": 17, "lineHeight": 22, "weight": 400 } }""",
        motion: String = """{ "launch": { "curve": "spring", "durationMs": 550, "bounce": 0.12 } }""",
    ): String {
        val merged = (validColors + colors).filterValues { it != null }
        val colorJson = merged.entries.joinToString(",\n") { (role, pair) ->
            """"$role": { "light": "${pair!!.first}", "dark": "${pair.second}" }"""
        }
        return """
            {
              "schemaVersion": $schemaVersion,
              "color": { $colorJson },
              "radius": $radius,
              "spacing": { "small": 8 },
              "layout": { "pageMargin": 18 },
              "opacity": $opacity,
              "typography": $typography,
              "motion": $motion
            }
        """.trimIndent()
    }

    /** Parses [json] and returns the rule violations. */
    fun violationsOf(json: String): List<String> = DesignTokenRules.violations(DesignTokenParser.parse(json))
}
