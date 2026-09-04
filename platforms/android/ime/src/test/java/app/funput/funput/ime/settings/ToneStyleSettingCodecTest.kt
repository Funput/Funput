package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ToneStyleSettingCodecTest {
    @Test
    fun `missing or unknown setting falls back to the caller's default`() {
        assertEquals(ToneStyle.TRADITIONAL, ToneStyleSettingCodec.decode(null))
        assertEquals(ToneStyle.TRADITIONAL, ToneStyleSettingCodec.decode("UNKNOWN"))
        assertEquals(ToneStyle.MODERN, ToneStyleSettingCodec.decode(null, ToneStyle.MODERN))
    }

    @Test
    fun `both supported styles round trip`() {
        ToneStyle.entries.forEach { style ->
            val encoded = ToneStyleSettingCodec.encode(style)

            assertEquals(style, ToneStyleSettingCodec.decode(encoded))
        }
    }

    @Test
    fun `native values map to the expected styles`() {
        assertEquals(ToneStyle.TRADITIONAL, ToneStyle.fromNativeValue(0))
        assertEquals(ToneStyle.MODERN, ToneStyle.fromNativeValue(1))
        assertEquals(ToneStyle.TRADITIONAL, ToneStyle.fromNativeValue(99))
    }
}
