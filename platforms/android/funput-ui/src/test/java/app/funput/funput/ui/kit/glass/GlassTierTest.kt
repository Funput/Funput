package app.funput.funput.ui.kit.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassTierTest {
    @Test
    fun `android 12 and newer get glass`() {
        assertEquals(GlassTier.GLASS, GlassTier.resolve(sdkInt = 31, isLowRamDevice = false))
        assertEquals(GlassTier.GLASS, GlassTier.resolve(sdkInt = 35, isLowRamDevice = false))
    }

    @Test
    fun `below android 12 the library cannot blur, so the surface is solid`() {
        assertEquals(GlassTier.SOLID, GlassTier.resolve(sdkInt = 26, isLowRamDevice = false))
        assertEquals(GlassTier.SOLID, GlassTier.resolve(sdkInt = 30, isLowRamDevice = false))
    }

    @Test
    fun `a low-ram device stays solid even on a new android`() {
        assertEquals(GlassTier.SOLID, GlassTier.resolve(sdkInt = 35, isLowRamDevice = true))
    }
}
