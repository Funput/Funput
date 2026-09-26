package app.funput.build.tokens

/**
 * What a parsed token set must satisfy beyond its shape.
 *
 * The contrast floors are the WCAG AA thresholds: 4.5:1 for body text, 3:1 for the accent, which
 * tints icons, controls and large labels. They are checked in both appearances, on both surfaces
 * the app draws on, so a token that only works on a white card cannot slip through.
 */
object DesignTokenRules {
    /** Colour roles every platform relies on; removing one would break generated code. */
    val RequiredColors: List<String> = listOf(
        "accent", "groupedBackground", "cardBackground", "cardStroke", "label", "secondaryLabel",
        "tertiaryLabel", "separator", "success", "destructive",
    )

    /** The two surfaces content sits on; both must be opaque. */
    val Surfaces: List<String> = listOf("groupedBackground", "cardBackground")

    /** Minimum contrast of body text ([label]) on every surface. */
    const val TEXT_CONTRAST: Double = 4.5

    /** Minimum contrast of the accent on every surface. */
    const val ACCENT_CONTRAST: Double = 3.0

    /** Every rule [tokens] breaks, as `path: reason`; empty when the set is valid. */
    fun violations(tokens: DesignTokens): List<String> = buildList {
        if (tokens.schemaVersion != DesignTokens.SUPPORTED_SCHEMA_VERSION) {
            add("schemaVersion: ${tokens.schemaVersion} is not supported " +
                "(expected ${DesignTokens.SUPPORTED_SCHEMA_VERSION})")
        }
        RequiredColors.filterNot(tokens.colors::containsKey).forEach { add("color.$it: missing") }
        addAll(surfaceViolations(tokens))
        addAll(contrastViolations(tokens, "label", TEXT_CONTRAST))
        addAll(contrastViolations(tokens, "accent", ACCENT_CONTRAST))
        addAll(numberViolations(tokens))
        addAll(typographyViolations(tokens))
        tokens.motion.forEach { (name, motion) ->
            if (motion.durationMs !in 1..2_000) add("motion.$name.durationMs: ${motion.durationMs} not in 1..2000")
            motion.bounce?.let { if (it !in 0.0..1.0) add("motion.$name.bounce: $it not in 0..1") }
        }
    }

    private fun surfaceViolations(tokens: DesignTokens) = Surfaces.flatMap { surface ->
        val pair = tokens.colors[surface] ?: return@flatMap emptyList()
        listOf("light" to pair.light, "dark" to pair.dark)
            .filterNot { (_, color) -> color.isOpaque }
            .map { (mode, _) -> "color.$surface.$mode: a surface must be opaque" }
    }

    private fun contrastViolations(tokens: DesignTokens, role: String, floor: Double): List<String> {
        val foreground = tokens.colors[role] ?: return emptyList()
        return Surfaces.flatMap { surface ->
            val background = tokens.colors[surface]?.takeIf { it.light.isOpaque && it.dark.isOpaque }
                ?: return@flatMap emptyList()
            listOf(
                Triple("light", foreground.light, background.light),
                Triple("dark", foreground.dark, background.dark),
            ).mapNotNull { (mode, top, bottom) ->
                val ratio = ContrastMath.ratio(top, bottom)
                if (ratio >= floor) null
                else "color.$role.$mode: %.2f:1 on $surface, needs %.1f:1".format(ratio, floor)
            }
        }
    }

    private fun numberViolations(tokens: DesignTokens) = tokens.numbers.flatMap { (group, values) ->
        values.mapNotNull { (name, value) ->
            val valid = if (group == "opacity") value in 0.0..1.0 else value >= 0.0
            if (valid) null else "$group.$name: $value is out of range"
        }
    }

    private fun typographyViolations(tokens: DesignTokens) = tokens.typography.flatMap { (name, style) ->
        listOfNotNull(
            "typography.$name.size: must be positive".takeIf { style.size <= 0.0 },
            "typography.$name.lineHeight: must not be smaller than the size"
                .takeIf { style.lineHeight < style.size },
            "typography.$name.weight: ${style.weight} is not a multiple of 100 in 100..900"
                .takeIf { style.weight !in 100..900 || style.weight % 100 != 0 },
        )
    }
}
