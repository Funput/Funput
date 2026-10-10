package app.funput.funput.keyboard.layout.microphone

import app.funput.funput.keyboard.SuggestionCapacity
import app.funput.funput.keyboard.accessibility.KeyboardAccessibilitySnapshot
import app.funput.funput.keyboard.interaction.interactionTargetAt
import app.funput.funput.keyboard.layout.KeyboardGeometry
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.letters.KeyboardLayouts
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState
import org.junit.Assert.*
import org.junit.Test

class MicrophoneToolbarTest {
    private val microphone = KeyboardMicrophoneState(true, true, "Nhập bằng giọng nói, Tiếng Anh")

    @Test fun microphoneStaysOnTheLeftWhenOptionalUtilitiesYieldToCandidates() {
        val full = resolve(360f, true)
        val candidates = resolve(360f, false)
        val before = requireNotNull(full.suggestionBar)
        val after = requireNotNull(candidates.suggestionBar)
        assertEquals(before.microphoneKey?.spec, after.microphoneKey?.spec)
        assertEquals(requireNotNull(before.microphoneKey).bounds.width,
            requireNotNull(after.microphoneKey).bounds.width, 0.01f)
        assertEquals(before.emojiKey.bounds, after.emojiKey.bounds)
        assertNull(after.clipboardKey)
        assertNull(after.placementKey)
        assertTrue(after.suggestionsBounds.width > before.suggestionsBounds.width)
        assertTrue(SuggestionCapacity.visibleCount(after.suggestionsBounds.width, 1f, 3) > 0)
    }

    @Test fun disablingSuggestionsKeepsMicrophoneAndDoesNotAlterRowsOrHeight() {
        val normal = resolve(240f, false)
        val without = resolve(240f, false, suggestions = false)
        assertEquals(normal.rows, without.rows)
        assertEquals(normal.height, without.height)
        assertEquals(normal.suggestionBar?.microphoneKey?.bounds, without.suggestionBar?.microphoneKey?.bounds)
        assertTrue(requireNotNull(without.suggestionBar?.microphoneKey).hitBounds.width >= 48f)
        assertFalse(requireNotNull(without.suggestionBar).suggestionsEnabled)
    }

    @Test fun narrowSurfacesDropUtilitiesWithoutShrinkingOrOverlappingMic() {
        listOf(120f, 160f, 240f, 360f).forEach { width ->
            val keyboard = resolve(width, true)
            val bar = requireNotNull(keyboard.suggestionBar)
            val mic = requireNotNull(bar.microphoneKey)
            assertEquals(48f, mic.bounds.width, 0.01f)
            assertTrue(mic.hitBounds.width >= 48f)
            assertTrue(mic.bounds.left >= bar.bounds.left)
            val controls = keyboard.keys.filter { it.bounds.top == bar.bounds.top }
            controls.zipWithNext().forEach { (left, right) ->
                assertTrue(left.hitBounds.right <= right.hitBounds.left)
            }
            assertTrue(bar.suggestionsBounds.width >= 0f)
            assertEquals(mic.spec.id, keyboard.interactionTargetAt(mic.bounds.centerX, mic.bounds.centerY, 3))
        }
        assertNull(resolve(120f, true).suggestionBar?.placementKey)
    }

    @Test fun accessibilitySharesHitBoundsLabelAndActiveState() {
        val keyboard = resolve(240f, false)
        val mic = requireNotNull(keyboard.suggestionBar?.microphoneKey)
        val snapshot = KeyboardAccessibilitySnapshot(keyboard, ShiftState.OFF, listOf("xin", "chào"))
        val node = requireNotNull(snapshot.nodes.firstOrNull { it.keyId == mic.spec.id })
        assertEquals(microphone.accessibilityLabel, node.label)
        assertEquals(mic.hitBounds, node.hitBounds)
        assertTrue(node.selected)
        assertEquals(node, snapshot.nodeAt(mic.bounds.centerX, mic.bounds.centerY))
        assertEquals(KeyRole.MICROPHONE, mic.spec.role)
    }

    @Test fun clipboardHintYieldsWhenItsLabelCannotFitBesideTheMicrophone() {
        assertFalse(requireNotNull(resolve(120f, false).suggestionBar).clipboardHintFits)
        assertTrue(requireNotNull(resolve(240f, false).suggestionBar).clipboardHintFits)
    }

    @Test fun defaultAndToolbarlessLayoutsHaveNoMicrophone() {
        val layout = KeyboardLayouts.telex
        val spec = KeyboardGeometrySpec.fromDensity(1f)
        assertNull(KeyboardGeometry.resolve(layout, 360f, 260f, spec).suggestionBar?.microphoneKey)
        assertNull(KeyboardGeometry.resolve(layout.copy(suggestionBar = null), 360f, 260f,
            spec, microphone = microphone).suggestionBar)
    }

    @Test fun heightScaleDoesNotReduceHorizontalMicrophoneTarget() {
        listOf(0.75f, 1f, 1.3f).forEach { scale ->
            val profile = KeyboardSizingProfile.Default.copy(heightScale = scale)
            val keyboard = KeyboardGeometry.resolve(KeyboardLayouts.telex, 240f, 300f,
                KeyboardGeometrySpec.fromProfile(1f, profile), microphone = microphone)
            assertEquals(48f, requireNotNull(keyboard.suggestionBar?.microphoneKey).bounds.width, 0.01f)
        }
    }

    private fun resolve(width: Float, utilities: Boolean, suggestions: Boolean = true) =
        KeyboardGeometry.resolve(KeyboardLayouts.telex.let { layout -> layout.copy(
            suggestionBar = requireNotNull(layout.suggestionBar).copy(suggestionsEnabled = suggestions),
        ) }, width, 260f, KeyboardGeometrySpec.fromDensity(1f),
            showClipboard = utilities, showPlacement = utilities, microphone = microphone)
}
