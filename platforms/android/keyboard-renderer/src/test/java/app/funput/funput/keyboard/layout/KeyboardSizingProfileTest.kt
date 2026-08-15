package app.funput.funput.keyboard.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSizingProfileTest {
    @Test
    fun defaultSitsAboveNormalAndInsideTheOfferedRange() {
        assertEquals(KeyboardSizingProfile.DefaultScale, KeyboardSizingProfile.Default.heightScale)
        assertEquals(1f, KeyboardSizingProfile.Normal.heightScale)
        assertEquals(0.96f, KeyboardSizingProfile.Normal.labelScale, 0.0001f)
    }

    @Test
    fun scaledClampsToTheOfferedRange() {
        assertEquals(
            KeyboardSizingProfile.MinScale,
            KeyboardSizingProfile.scaled(0.1f).heightScale,
        )
        assertEquals(
            KeyboardSizingProfile.MaxScale,
            KeyboardSizingProfile.scaled(9f).heightScale,
        )
    }

    @Test
    fun labelsFollowTheKeyHeight() {
        assertEquals(1.04f, KeyboardSizingProfile.scaled(1.08f).labelScale, 0.0001f)
        assertEquals(0.88f, KeyboardSizingProfile.scaled(0.92f).labelScale, 0.0001f)
    }

    @Test
    fun phoneLandscapeClampsDownToItsCeiling() {
        val tall = KeyboardSizingProfile.scaled(1.2f)
        assertEquals(
            KeyboardSizingProfile.PhoneLandscapeMaxScale,
            tall.constrainedForLandscape(isPhoneLandscape = true).heightScale,
        )
        assertEquals(1.2f, tall.constrainedForLandscape(isPhoneLandscape = false).heightScale)
    }

    @Test
    fun phoneLandscapeLeavesSmallerScalesUntouched() {
        val small = KeyboardSizingProfile.scaled(0.85f)
        assertEquals(small, small.constrainedForLandscape(isPhoneLandscape = true))
    }
}
