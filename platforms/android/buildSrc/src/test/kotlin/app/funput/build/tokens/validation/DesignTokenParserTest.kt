package app.funput.build.tokens.validation

import app.funput.build.tokens.model.MotionCurve
import app.funput.build.tokens.model.MotionToken
import app.funput.build.tokens.model.RgbaColor
import app.funput.build.tokens.model.TextStyleToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokenParserTest {
    @Test
    fun `parses every group of a valid file`() {
        val tokens = DesignTokenParser.parse(TokenFixtures.json())

        assertEquals(1, tokens.schemaVersion)
        assertEquals(RgbaColor(0xD4, 0x6B, 0x08), tokens.colors.getValue("accent").light)
        assertEquals(RgbaColor(0xFF, 0xFF, 0xFF, 0x12), tokens.colors.getValue("cardStroke").dark)
        assertEquals(22.0, tokens.numbers.getValue("radius").getValue("card"), 0.0)
        assertEquals(TextStyleToken(17.0, 22.0, 400), tokens.typography.getValue("body"))
        assertEquals(MotionToken(MotionCurve.SPRING, 550, 0.12), tokens.motion.getValue("launch"))
    }

    @Test
    fun `hex colours accept six or eight digits and nothing else`() {
        assertEquals(RgbaColor(0x12, 0x34, 0x56), RgbaColor.parse("#123456"))
        assertEquals(RgbaColor(0x12, 0x34, 0x56, 0x78), RgbaColor.parse("#12345678"))
        listOf("123456", "#12345", "#1234567", "#GG0000", "#123456789").forEach { text ->
            assertNull(text, RgbaColor.parse(text))
        }
    }

    @Test
    fun `reports every structural problem at once, each with its path`() {
        val json = TokenFixtures.json(
            colors = mapOf("accent" to ("orange" to "#FFA43F")),
            radius = """{ "card": "big" }""",
            motion = """{ "launch": { "curve": "wobble", "durationMs": 100 } }""",
        )

        val problems = assertThrows(DesignTokenException::class.java) { DesignTokenParser.parse(json) }.problems

        assertEquals(3, problems.size)
        assertTrue(problems[0], problems.any { it.startsWith("color.accent.light:") })
        assertTrue(problems.any { it.startsWith("radius.card:") })
        assertTrue(problems.any { it.startsWith("motion.launch.curve:") })
    }

    @Test
    fun `a spring without a bounce is incomplete`() {
        val json = TokenFixtures.json(motion = """{ "launch": { "curve": "spring", "durationMs": 300 } }""")

        val problems = assertThrows(DesignTokenException::class.java) { DesignTokenParser.parse(json) }.problems

        assertEquals(listOf("motion.launch.bounce: a spring needs a bounce"), problems)
    }

    @Test
    fun `invalid JSON is one clear problem, not a stack trace`() {
        val problems = assertThrows(DesignTokenException::class.java) {
            DesignTokenParser.parse("{ not json")
        }.problems

        assertEquals(1, problems.size)
        assertTrue(problems.single().startsWith("<file>: not valid JSON"))
    }
}
