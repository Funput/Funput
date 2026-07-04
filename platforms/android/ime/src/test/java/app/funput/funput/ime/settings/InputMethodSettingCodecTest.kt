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
    fun `both supported methods round trip`() {
        KeyboardInputMethod.entries.forEach { method ->
            val encoded = InputMethodSettingCodec.encode(method)

            assertEquals(method, InputMethodSettingCodec.decode(encoded))
        }
    }
}
