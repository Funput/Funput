package app.funput.build.tokens

import app.funput.build.tokens.TokenFixtures.json
import app.funput.build.tokens.TokenFixtures.violationsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokenRulesTest {
    @Test
    fun `the fixture passes every rule`() {
        assertEquals(emptyList<String>(), violationsOf(json()))
    }

    @Test
    fun `an unsupported schema version is rejected`() {
        assertEquals(listOf("schemaVersion: 2 is not supported (expected 1)"), violationsOf(json(schemaVersion = "2")))
    }

    @Test
    fun `every required colour role must exist`() {
        assertEquals(listOf("color.separator: missing"), violationsOf(json(colors = mapOf("separator" to null))))
    }

    @Test
    fun `surfaces must be opaque`() {
        val violations = violationsOf(json(colors = mapOf("cardBackground" to ("#FFFFFF80" to "#1C1C1E"))))

        assertEquals(listOf("color.cardBackground.light: a surface must be opaque"), violations)
    }

    @Test
    fun `an accent below 3 to 1 fails in the appearance where it is weak`() {
        // The raw Funput seed: fine on dark surfaces, 2.5:1 on a white card.
        val violations = violationsOf(json(colors = mapOf("accent" to ("#EF8A1A" to "#FFA43F"))))

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.startsWith("color.accent.light:") })
        assertTrue(violations.any { it.contains("on cardBackground") })
    }

    @Test
    fun `body text needs 4_5 to 1 on both surfaces`() {
        val violations = violationsOf(json(colors = mapOf("label" to ("#000000" to "#555555"))))

        assertEquals(2, violations.size)
        assertTrue(violations.all { it.startsWith("color.label.dark:") })
    }

    @Test
    fun `opacity lives in 0 to 1 and other numbers are never negative`() {
        val violations = violationsOf(json(radius = """{ "card": -1 }""", opacity = """{ "tintFill": 1.5 }"""))

        assertEquals(listOf("radius.card: -1.0 is out of range", "opacity.tintFill: 1.5 is out of range"), violations)
    }

    @Test
    fun `a line height shorter than its text would clip stacked diacritics`() {
        val violations = violationsOf(json(typography = """{ "body": { "size": 17, "lineHeight": 16, "weight": 450 } }"""))

        assertEquals(
            listOf(
                "typography.body.lineHeight: must not be smaller than the size",
                "typography.body.weight: 450 is not a multiple of 100 in 100..900",
            ),
            violations,
        )
    }

    @Test
    fun `motion stays within a sane duration and bounce`() {
        val motion = """{ "launch": { "curve": "spring", "durationMs": 5000, "bounce": 1.5 } }"""

        assertEquals(
            listOf("motion.launch.durationMs: 5000 not in 1..2000", "motion.launch.bounce: 1.5 not in 0..1"),
            violationsOf(json(motion = motion)),
        )
    }
}
