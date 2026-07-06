package app.funput.funput.ui.theme

import app.funput.funput.ime.settings.AppearanceMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceModeTest {
    @Test
    fun `system mode follows the device`() {
        assertTrue(AppearanceMode.SYSTEM.resolveDarkTheme(systemDarkTheme = true))
        assertFalse(AppearanceMode.SYSTEM.resolveDarkTheme(systemDarkTheme = false))
    }

    @Test
    fun `explicit appearance overrides the device`() {
        assertFalse(AppearanceMode.LIGHT.resolveDarkTheme(systemDarkTheme = true))
        assertTrue(AppearanceMode.DARK.resolveDarkTheme(systemDarkTheme = false))
    }
}
