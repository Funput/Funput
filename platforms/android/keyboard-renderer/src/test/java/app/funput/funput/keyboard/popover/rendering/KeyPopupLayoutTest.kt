package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyPopupLayoutTest {
    @Test
    fun `printable keys are eligible outside secure editors`() {
        assertTrue(KeyPopupLayout.isEligible(key(KeyRole.CHARACTER, "a"), secure = false))
        assertTrue(KeyPopupLayout.isEligible(key(KeyRole.VNI_MODIFIER, "1"), secure = false))
        assertTrue(KeyPopupLayout.isEligible(key(KeyRole.PUNCTUATION, "."), secure = false))
        assertFalse(KeyPopupLayout.isEligible(key(KeyRole.BACKSPACE, "Backspace"), secure = false))
    }

    @Test
    fun `secure editors suppress printable key popups`() {
        assertFalse(KeyPopupLayout.isEligible(key(KeyRole.CHARACTER, "a"), secure = true))
    }

    @Test
    fun `popup label follows active shift state`() {
        val key = key(KeyRole.CHARACTER, "a", shiftedLabel = "A")

        assertEquals("a", KeyPopupLayout.label(key, ShiftState.OFF))
        assertEquals("A", KeyPopupLayout.label(key, ShiftState.ON))
        assertEquals("A", KeyPopupLayout.label(key, ShiftState.CAPS_LOCK))
    }

    @Test
    fun `popup stays inside horizontal surface edges`() {
        val left = KeyPopupLayout.bounds(
            key(KeyRole.CHARACTER, "a", KeyBounds(0f, 100f, 40f, 140f)),
            surfaceWidth = 200f,
            popupWidth = 60f,
            popupHeight = 70f,
            edgeInset = 4f,
            anchorOverlap = 6f,
        )
        val right = KeyPopupLayout.bounds(
            key(KeyRole.CHARACTER, "l", KeyBounds(160f, 100f, 200f, 140f)),
            surfaceWidth = 200f,
            popupWidth = 60f,
            popupHeight = 70f,
            edgeInset = 4f,
            anchorOverlap = 6f,
        )

        assertEquals(4f, left.left)
        assertEquals(196f, right.right)
        assertEquals(106f, left.bottom)
    }

    @Test
    fun `top row popup remains visible inside the canvas`() {
        val bounds = KeyPopupLayout.bounds(
            key(KeyRole.CHARACTER, "q", KeyBounds(20f, 4f, 60f, 44f)),
            surfaceWidth = 200f,
            popupWidth = 60f,
            popupHeight = 70f,
            edgeInset = 4f,
            anchorOverlap = 6f,
        )

        assertEquals(4f, bounds.top)
        assertEquals(74f, bounds.bottom)
    }

    private fun key(
        role: KeyRole,
        label: String,
        bounds: KeyBounds = KeyBounds(80f, 100f, 120f, 140f),
        shiftedLabel: String? = null,
    ) = ResolvedKey(KeySpec("key-$label-$role", label, role, shiftedLabel = shiftedLabel), bounds)
}
