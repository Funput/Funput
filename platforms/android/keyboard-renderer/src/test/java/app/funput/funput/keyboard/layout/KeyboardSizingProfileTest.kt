package app.funput.funput.keyboard.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class KeyboardSizingProfileTest {
    @Test
    fun defaultProfileIsNormal() {
        assertSame(KeyboardSizingProfile.Normal, KeyboardSizingProfile.Default)
    }

    @Test
    fun presetsExposeDistinctHeightScales() {
        assertEquals(0.92f, KeyboardSizingProfile.Compact.heightScale)
        assertEquals(1f, KeyboardSizingProfile.Normal.heightScale)
        assertEquals(1.08f, KeyboardSizingProfile.Large.heightScale)
    }

    @Test
    fun fromIdResolvesKnownPresetsAndFallsBackToNormal() {
        assertSame(KeyboardSizingProfile.Compact, KeyboardSizingProfile.fromId("compact"))
        assertSame(KeyboardSizingProfile.Normal, KeyboardSizingProfile.fromId("normal"))
        assertSame(KeyboardSizingProfile.Large, KeyboardSizingProfile.fromId("large"))
        assertSame(KeyboardSizingProfile.Normal, KeyboardSizingProfile.fromId(null))
        assertSame(KeyboardSizingProfile.Normal, KeyboardSizingProfile.fromId("unknown"))
    }
}
