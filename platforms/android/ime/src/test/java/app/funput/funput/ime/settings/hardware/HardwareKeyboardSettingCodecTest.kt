package app.funput.funput.ime.settings.hardware

import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareKeyboardSettingCodecTest {
    @Test
    fun `missing values keep the hidden-by-default behaviour with the hotkey on`() {
        val decoded = HardwareKeyboardSettingCodec.decode(showsSoftKeyboard = null, toggleHotkeyEnabled = null)

        assertEquals(HardwareKeyboardPreferences.Default, decoded)
        assertEquals(false, decoded.showsSoftKeyboard)
        assertEquals(true, decoded.toggleHotkeyEnabled)
    }

    @Test
    fun `explicit values round trip independently`() {
        assertEquals(
            HardwareKeyboardPreferences(showsSoftKeyboard = true, toggleHotkeyEnabled = false),
            HardwareKeyboardSettingCodec.decode(showsSoftKeyboard = true, toggleHotkeyEnabled = false),
        )
        assertEquals(
            HardwareKeyboardPreferences(showsSoftKeyboard = false, toggleHotkeyEnabled = true),
            HardwareKeyboardSettingCodec.decode(showsSoftKeyboard = false, toggleHotkeyEnabled = true),
        )
    }
}
