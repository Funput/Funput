package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceSettingCodecTest {
    @Test
    fun `round trips every supported appearance`() {
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, AppearanceSettingCodec.decode(AppearanceSettingCodec.encode(mode)))
        }
    }

    @Test
    fun `uses system appearance for missing or unknown values`() {
        assertEquals(AppearanceMode.SYSTEM, AppearanceSettingCodec.decode(null))
        assertEquals(AppearanceMode.SYSTEM, AppearanceSettingCodec.decode("SEPIA"))
    }
}
