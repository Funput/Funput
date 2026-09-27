package app.funput.build.tokens.validation

import app.funput.build.tokens.model.DesignTokens

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
        "accent", "onAccent", "groupedBackground", "cardBackground", "cardStroke", "label", "secondaryLabel",
        "tertiaryLabel", "separator", "success", "destructive",
    )

    /** The two surfaces content sits on; both must be opaque. */
    val Surfaces: List<String> = listOf("groupedBackground", "cardBackground")

    /** Minimum contrast of body text ([label]) on every surface. */
    const val TEXT_CONTRAST: Double = 4.5

    /** Minimum contrast of the accent on every surface. */
    const val ACCENT_CONTRAST: Double = 3.0

    /** Token names become generated identifiers, so they must be lowerCamelCase. */
    private val TokenName = Regex("^[a-z][A-Za-z0-9]*$")

    /** Every rule [tokens] breaks, as `path: reason`; empty when the set is valid. */
    fun violations(tokens: DesignTokens): List<String> = buildList {
        if (tokens.schemaVersion != DesignTokens.SUPPORTED_SCHEMA_VERSION) {
            add("schemaVersion: ${tokens.schemaVersion} is not supported " +
                "(expected ${DesignTokens.SUPPORTED_SCHEMA_VERSION})")
        }
        RequiredColors.filterNot(tokens.colors::containsKey).forEach { add("color.$it: missing") }
        addAll(nameViolations(tokens))
        addAll(surfaceViolations(tokens))
        addAll(contrastViolations(tokens, "label", TEXT_CONTRAST))
        addAll(contrastViolations(tokens, "accent", ACCENT_CONTRAST))
        addAll(onAccentViolations(tokens))
        addAll(tintViolations(tokens))
        addAll(numberViolations(tokens))
        addAll(typographyViolations(tokens))
        tokens.motion.forEach { (name, motion) ->
            if (motion.durationMs !in 1..2_000) add("motion.$name.durationMs: ${motion.durationMs} not in 1..2000")
            motion.bounce?.let { if (it !in 0.0..1.0) add("motion.$name.bounce: $it not in 0..1") }
        }
    }

    private fun nameViolations(tokens: DesignTokens): List<String> {
        val groups = mapOf(
            "color" to tokens.colors.keys,
            "typography" to tokens.typography.keys,
            "motion" to tokens.motion.keys,
        ) + tokens.numbers.mapValues { (_, values) -> values.keys }
        return groups.flatMap { (group, names) ->
            names.filterNot(TokenName::matches).map { "$group.$it: name must be lowerCamelCase" }
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

    /**
     * Icon tints (`tint*`) colour glyphs drawn on cards, so each must reach the non-text floor on
     * the card colour in both appearances.
     */
    private fun tintViolations(tokens: DesignTokens): List<String> {
        val card = tokens.colors["cardBackground"]
            ?.takeIf { it.light.isOpaque && it.dark.isOpaque } ?: return emptyList()
        return tokens.colors.filterKeys { it.startsWith("tint") }.flatMap { (name, tint) ->
            listOf("light" to (tint.light to card.light), "dark" to (tint.dark to card.dark))
                .mapNotNull { (mode, pair) ->
                    val ratio = ContrastMath.ratio(pair.first, pair.second)
                    if (ratio >= ACCENT_CONTRAST) null
                    else "color.$name.$mode: %.2f:1 on cardBackground, needs %.1f:1".format(ratio, ACCENT_CONTRAST)
                }
        }
    }

    /** Text on an accent-filled button must read like body text, in both appearances. */
    private fun onAccentViolations(tokens: DesignTokens): List<String> {
        val text = tokens.colors["onAccent"] ?: return emptyList()
        val fill = tokens.colors["accent"]?.takeIf { it.light.isOpaque && it.dark.isOpaque } ?: return emptyList()
        return listOf("light" to (text.light to fill.light), "dark" to (text.dark to fill.dark))
            .mapNotNull { (mode, colors) ->
                val ratio = ContrastMath.ratio(colors.first, colors.second)
                if (ratio >= TEXT_CONTRAST) null
                else "color.onAccent.$mode: %.2f:1 on accent, needs %.1f:1".format(ratio, TEXT_CONTRAST)
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
