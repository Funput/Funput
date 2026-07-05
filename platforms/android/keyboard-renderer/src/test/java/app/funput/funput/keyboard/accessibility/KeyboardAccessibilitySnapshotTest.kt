package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.resolveGeometry
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardAccessibilitySnapshotTest {
    @Test
    fun `placeholder keys are hidden from accessibility`() {
        val snapshot = snapshot(editorMode = KeyboardEditorMode.PHONE)

        assertFalse(snapshot.nodes.any { it.keyId.startsWith("placeholder-") })
        assertTrue(snapshot.nodes.any { it.keyId == "keypad-digit-0" })
    }

    @Test
    fun `shift updates character labels and selected state`() {
        val normal = snapshot(shiftState = ShiftState.OFF)
        val shifted = snapshot(shiftState = ShiftState.ON)

        assertEquals("q", normal.nodes.first { it.keyId == "character-q" }.label)
        assertEquals("Q", shifted.nodes.first { it.keyId == "character-q" }.label)
        assertTrue(shifted.nodes.first { it.keyId == "shift" }.selected)
    }

    @Test
    fun `toolbar and system switcher are exposed as virtual nodes`() {
        val snapshot = snapshot(systemSwitcherVisible = true)

        assertTrue(snapshot.nodes.any { it.keyId == "settings" })
        assertTrue(snapshot.nodes.any { it.keyId == "emoji" })
        assertTrue(snapshot.nodes.any { it.keyId == "system-input-method" })
    }

    @Test
    fun `hit testing resolves a virtual node`() {
        val snapshot = snapshot()
        val key = snapshot.nodes.first { it.keyId == "space" }

        assertNotNull(snapshot.nodeAt(key.hitBounds.centerX, key.hitBounds.centerY))
        assertEquals(key, snapshot.node(key.virtualId))
    }

    private fun snapshot(
        shiftState: ShiftState = ShiftState.OFF,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        systemSwitcherVisible: Boolean = false,
    ): KeyboardAccessibilitySnapshot {
        val profile = KeyboardSizingProfile.Normal
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = KeyboardInputMethod.VNI,
            mode = KeyboardLayoutMode.LETTERS,
            editorMode = editorMode,
            systemInputMethodSwitcherVisible = systemSwitcherVisible,
        )
        val keyboard = requireNotNull(layout.resolveGeometry(
            width = KeyboardDimensions.DefaultWidthDp.toInt(),
            height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, editorMode, profile).toInt(),
            density = 1f,
            profile = profile,
        ))
        return KeyboardAccessibilitySnapshot(keyboard, shiftState)
    }
}
