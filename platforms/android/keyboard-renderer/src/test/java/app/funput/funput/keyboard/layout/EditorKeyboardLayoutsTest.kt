package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class EditorKeyboardLayoutsTest {
    @Test
    fun `email layout exposes email punctuation with top number row`() {
        KeyboardInputMethod.entries.forEach { method ->
            val layout = resolve(method, KeyboardEditorMode.EMAIL)

            assertEquals(5, layout.rows.size)
            assertEquals("1234567890", layout.rows.first().keys.joinToString("") { it.label })
            assertEquals(listOf("@", "."), actionLabels(layout).filter { it in EmailLabels })
            assertNull(layout.rows.last().keys.first { it.id == "space" }.horizontalSwipeAction)
        }
    }

    @Test
    fun `search layout exposes slash and period with top number row`() {
        val layout = resolve(KeyboardInputMethod.VNI, KeyboardEditorMode.SEARCH)
        val space = layout.rows.last().keys.first { it.id == "space" }

        assertEquals(5, layout.rows.size)
        assertEquals("1234567890", layout.rows.first().keys.joinToString("") { it.label })
        assertEquals(listOf("/", "."), actionLabels(layout).filter { it in UrlLabels })
        assertEquals(KeySwipeAction.TOGGLE_LANGUAGE, space.horizontalSwipeAction)
    }

    @Test
    fun `URL layout exposes slash and period with top number row`() {
        val layout = resolve(KeyboardInputMethod.VNI, KeyboardEditorMode.URL)
        val space = layout.rows.last().keys.first { it.id == "space" }

        assertEquals(5, layout.rows.size)
        assertEquals(listOf("/", "."), actionLabels(layout).filter { it in UrlLabels })
        assertNull(space.horizontalSwipeAction)
    }

    @Test
    fun `web editor layouts give space the same width as iOS`() {
        val editorModes = listOf(
            KeyboardEditorMode.EMAIL,
            KeyboardEditorMode.SEARCH,
            KeyboardEditorMode.URL,
        )

        KeyboardInputMethod.entries.forEach { method ->
            editorModes.forEach { editorMode ->
                val space = resolve(method, editorMode).rows.last().keys.first { it.role == KeyRole.SPACE }

                assertEquals("$method $editorMode", 5.8f, space.widthWeight)
            }
        }
    }

    @Test
    fun `QWERTY layouts share the same five row height`() {
        val expected = KeyboardDimensions.heightForRowCount(rowCount = 5, hasSuggestionBar = true)
        assertEquals(
            expected,
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.EMAIL),
        )
        assertEquals(
            expected,
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI, KeyboardEditorMode.SEARCH),
        )
        assertEquals(
            expected,
            KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.TELEX, KeyboardEditorMode.TEXT),
        )
    }

    @Test
    fun `disabled suggestion policy keeps emoji toolbar only`() {
        val layout = KeyboardLayoutResolver.resolve(
            KeyboardInputMethod.VNI,
            KeyboardLayoutMode.LETTERS,
            KeyboardEditorMode.TEXT,
            suggestionsEnabled = false,
        )
        val bar = requireNotNull(layout.suggestionBar)

        assertEquals(KeyRole.EMOJI, bar.emojiKey.role)
        assertFalse(bar.suggestionsEnabled)
    }

    private fun resolve(method: KeyboardInputMethod, editorMode: KeyboardEditorMode) =
        KeyboardLayoutResolver.resolve(method, KeyboardLayoutMode.LETTERS, editorMode)

    private fun actionLabels(layout: app.funput.funput.keyboard.model.KeyboardLayout) =
        layout.rows.last().keys.map { it.label }

    private companion object {
        val EmailLabels = setOf("@", ".", ".com")
        val UrlLabels = setOf("/", ".", ".com")
    }
}
