package app.funput.build.tokens.codegen

import app.funput.build.tokens.validation.DesignTokenParser
import app.funput.build.tokens.validation.TokenFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinTokenEmitterTest {
    private val source = KotlinTokenEmitter("app.example.tokens").emit(DesignTokenParser.parse(TokenFixtures.json()))

    @Test
    fun `declares the package and marks itself generated`() {
        val lines = source.lines()

        assertTrue(lines[0].startsWith("// Generated from design/tokens/app.tokens.json"))
        assertEquals("package app.example.tokens", lines[1])
    }

    @Test
    fun `colours become ARGB literals in both appearances`() {
        assertTrue(source.contains("val accent = ColorToken(light = Color(0xFFD46B08), dark = Color(0xFFFFA43F))"))
        // CSS-order #RRGGBBAA must come out alpha-first.
        assertTrue(source.contains("val cardStroke = ColorToken(light = Color(0x12000000), dark = Color(0x12FFFFFF))"))
    }

    @Test
    fun `dimensions are dp and opacity is a plain float`() {
        assertTrue(source.contains("internal object RadiusTokens {\n    val card = 22.dp\n}"))
        assertTrue(source.contains("internal object OpacityTokens {\n    val tintFill = 0.12f\n}"))
    }

    @Test
    fun `type and motion keep every field`() {
        assertTrue(source.contains("val body = TypeToken(size = 17.sp, lineHeight = 22.sp, weight = FontWeight(400))"))
        assertTrue(
            source.contains("val launch = MotionToken(curve = TokenCurve.SPRING, durationMillis = 550, bounce = 0.12f)"),
        )
    }

    @Test
    fun `the same tokens always produce the same file`() {
        val again = KotlinTokenEmitter("app.example.tokens").emit(DesignTokenParser.parse(TokenFixtures.json()))

        assertEquals(source, again)
    }
}
