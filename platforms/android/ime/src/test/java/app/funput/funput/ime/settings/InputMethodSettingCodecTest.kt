package app.funput.funput.ime.settings

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class InputMethodSettingCodecTest {
    @Test
    fun `missing or unknown setting falls back to VNI`() {
        assertEquals(KeyboardInputMethod.VNI, InputMethodSettingCodec.decode(null))
        assertEquals(KeyboardInputMethod.VNI, InputMethodSettingCodec.decode("UNKNOWN"))
    }

    @Test
    fun `all supported methods use stable persisted values`() {
        assertEquals("telex", InputMethodSettingCodec.encode(KeyboardInputMethod.TELEX))
        assertEquals("telex_advanced", InputMethodSettingCodec.encode(KeyboardInputMethod.TELEX_ADVANCED))
        assertEquals("vni", InputMethodSettingCodec.encode(KeyboardInputMethod.VNI))

        KeyboardInputMethod.entries.forEach { method ->
            val encoded = InputMethodSettingCodec.encode(method)

            assertEquals(method, InputMethodSettingCodec.decode(encoded))
        }
    }

    @Test
    fun `legacy uppercase values remain readable`() {
        assertEquals(KeyboardInputMethod.TELEX, InputMethodSettingCodec.decode("TELEX"))
        assertEquals(KeyboardInputMethod.VNI, InputMethodSettingCodec.decode("VNI"))
    }
}
