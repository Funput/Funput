package app.funput.funput.keyboard.layout.toolbar

import app.funput.funput.keyboard.layout.KeyboardGeometry
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.letters.KeyboardLayouts
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarSlotLayoutTest {
    private val microphone = KeyboardMicrophoneState(true, true, "Nhập bằng giọng nói")

    @Test
    fun `default arrangement matches the historical left mic right emoji layout`() {
        val bar = resolve(ToolbarArrangement.Default)
        val mic = requireNotNull(bar.microphoneKey).bounds
        val emoji = bar.emojiKey.bounds
        assertEquals(bar.bounds.left, mic.left, 0.01f)
        assertEquals(bar.bounds.right, emoji.right, 0.01f)
        assertTrue(mic.right <= requireNotNull(bar.placementKey).bounds.left)
        assertTrue(requireNotNull(bar.clipboardKey).bounds.right <= emoji.left)
    }

    @Test
    fun `swapping mic and emoji sides moves each to the opposite edge`() {
        val arrangement = ToolbarArrangement(
            leading = listOf(ToolbarSlot.EMOJI, ToolbarSlot.CLIPBOARD),
            trailing = listOf(ToolbarSlot.PLACEMENT, ToolbarSlot.MICROPHONE),
        )
        val bar = resolve(arrangement)
        assertEquals(bar.bounds.left, bar.emojiKey.bounds.left, 0.01f)
        assertEquals(bar.bounds.right, requireNotNull(bar.microphoneKey).bounds.right, 0.01f)
    }

    @Test
    fun `trailing list order is left to right on screen`() {
        val arrangement = ToolbarArrangement(
            leading = listOf(ToolbarSlot.MICROPHONE),
            trailing = listOf(ToolbarSlot.CLIPBOARD, ToolbarSlot.PLACEMENT, ToolbarSlot.EMOJI),
        )
        val bar = resolve(arrangement)
        val clipboard = requireNotNull(bar.clipboardKey).bounds
        val placement = requireNotNull(bar.placementKey).bounds
        val emoji = bar.emojiKey.bounds
        assertTrue(clipboard.right <= placement.left)
        assertTrue(placement.right <= emoji.left)
        assertEquals(bar.bounds.right, emoji.right, 0.01f)
    }

    @Test
    fun `optional utilities still yield on a narrow surface`() {
        val bar = resolve(ToolbarArrangement.Default, width = 120f)
        assertNull(bar.placementKey)
        assertNull(bar.clipboardKey)
        assertEquals(bar.bounds.left, requireNotNull(bar.microphoneKey).bounds.left, 0.01f)
        assertEquals(bar.bounds.right, bar.emojiKey.bounds.right, 0.01f)
    }

    private fun resolve(arrangement: ToolbarArrangement, width: Float = 360f) =
        requireNotNull(
            KeyboardGeometry.resolve(
                KeyboardLayouts.telex,
                width,
                260f,
                KeyboardGeometrySpec.fromDensity(1f),
                showClipboard = true,
                showPlacement = true,
                microphone = microphone,
                arrangement = arrangement,
            ).suggestionBar,
        )
}
