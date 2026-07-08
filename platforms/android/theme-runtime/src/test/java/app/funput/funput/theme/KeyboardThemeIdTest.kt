package app.funput.funput.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyboardThemeIdTest {
    @Test
    fun acceptsStablePackageStyleIdentifiers() {
        val id = KeyboardThemeId.of("com.funput.theme.aurora-glass_v2")

        assertEquals("com.funput.theme.aurora-glass_v2", id.value)
    }

    @Test
    fun parseRejectsMalformedExternalValues() {
        assertNull(KeyboardThemeId.parseOrNull(null))
        assertNull(KeyboardThemeId.parseOrNull(""))
        assertNull(KeyboardThemeId.parseOrNull(" Light"))
        assertNull(KeyboardThemeId.parseOrNull("LIGHT"))
        assertNull(KeyboardThemeId.parseOrNull("theme/../../invalid"))
    }

    @Test
    fun factoryRejectsMalformedProgrammerInput() {
        assertThrows(IllegalArgumentException::class.java) {
            KeyboardThemeId.of("invalid theme")
        }
    }
}
