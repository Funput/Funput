package app.funput.funput.keyboard.accessibility

import app.funput.funput.keyboard.suggestion.ClipboardOfferChip
import app.funput.funput.keyboard.suggestion.asSuggestionChips
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.geometry.resolveGeometry
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

    @Test
    fun `personal candidates are exposed as full height virtual nodes`() {
        val snapshot = snapshot(suggestions = listOf("xin", "chào", "bạn"))
        val candidates = snapshot.nodes.filter { it.keyId.startsWith("suggestion-") }

        assertEquals(listOf("Gợi ý, xin", "Gợi ý, chào", "Gợi ý, bạn"), candidates.map { it.label })
        assertEquals(3, candidates.size)
        assertTrue(candidates.all { it.hitBounds.height > 0f })
    }

    @Test
    fun `clipboard key exposes paste label and history action`() {
        val node = snapshot(clipboardKeyLabel = "Dán, Đã sao chép văn bản")
            .nodes.single { it.keyId == "clipboard" }
        assertEquals("Dán, Đã sao chép văn bản", node.label)
        assertTrue(node.customActions.any { it.actionId == ClipboardAccessibility.OpenHistory })
    }

    @Test
    fun `paste chip is one node across the band until candidates replace it`() {
        val chip = snapshot(clipboardChipLabel = "Dán, xin chào").nodes.single { it.keyId == ClipboardOfferChip.TargetId }
        assertEquals("Dán, xin chào", chip.label)
        assertNotNull(snapshot(clipboardChipLabel = "Dán, xin chào").nodeAt(chip.bounds.centerX, chip.bounds.centerY))
        val typing = snapshot(suggestions = listOf("xin"), clipboardChipLabel = "Dán, xin chào")
        assertFalse(typing.nodes.any { it.keyId == ClipboardOfferChip.TargetId })
    }

    @Test
    fun `letter alternates become shifted TalkBack actions`() {
        val actions = snapshot(shiftState = ShiftState.ON).nodes
            .first { it.keyId == "character-a" }.alternateActions

        assertEquals("Chọn A", actions.first().label)
        assertTrue(actions.any { it.label == "Chọn Ắ" })
        assertTrue(actions.any { it.label == "Chọn Ậ" })
        assertEquals(18, actions.size)
    }

    @Test
    fun `period exposes all punctuation alternates to TalkBack`() {
        val actions = snapshot().nodes.first { it.keyId == "period" }.alternateActions

        assertEquals(15, actions.size)
        assertEquals("Chọn a còng", actions.first().label)
        assertEquals("Chọn dấu hỏi", actions[14].label)
        assertTrue(actions.none { it.label == "Chọn dấu phẩy" })
    }

    @Test
    fun `comma exposes named function actions to TalkBack`() {
        val actions = snapshot().nodes.first { it.keyId == "comma" }.alternateActions
        assertEquals(
            listOf("Tùy chỉnh vị trí bàn phím", "Cài đặt, mở ứng dụng Funput"),
            actions.map { it.label },
        )
    }

    @Test
    fun `excluded editors expose no alternate actions`() {
        val node = snapshot(editorMode = KeyboardEditorMode.URL).nodes
            .first { it.keyId == "character-a" }

        assertTrue(node.alternateActions.isEmpty())
    }

    private fun snapshot(
        shiftState: ShiftState = ShiftState.OFF,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        systemSwitcherVisible: Boolean = false,
        suggestions: List<String> = emptyList(),
        clipboardKeyLabel: String? = null,
        clipboardChipLabel: String? = null,
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
            showClipboard = true,
        ))
        return KeyboardAccessibilitySnapshot(
            keyboard, shiftState, suggestions.asSuggestionChips(), clipboardKeyLabel,
            clipboardChipLabel = clipboardChipLabel,
        )
    }
}
