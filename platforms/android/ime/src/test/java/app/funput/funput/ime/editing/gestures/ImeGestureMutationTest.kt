package app.funput.funput.ime.editing.gestures

import app.funput.funput.keyboard.model.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Test

class ImeGestureMutationTest {
    @Test
    fun deletingAWordRemovesItTogetherWithTheSpaceBeforeTheCaret() {
        val document = GestureDocument("xin chào ")
        handler(document).onKeyAction(KeyAction.DeleteWord)
        assertEquals("xin ", document.text)
    }

    @Test
    fun aVietnameseWordWithDiacriticsIsRemovedWhole() {
        val document = GestureDocument("xin chào")
        handler(document).onKeyAction(KeyAction.DeleteWord)
        assertEquals("xin ", document.text)
    }

    @Test
    fun deletingAWordDoesNotReopenItForRetoning() {
        val document = GestureDocument("xin chào")
        val actions = handler(document)
        actions.onKeyAction(KeyAction.DeleteWord)
        actions.onKeyAction(KeyAction.Input("character-s", "s"))
        assertEquals("xin s", document.text)
    }

    @Test
    fun typingAfterAMoveInsertsAtTheNewCaret() {
        val document = GestureDocument("xin chao")
        val actions = handler(document)
        actions.onKeyAction(KeyAction.MoveCursor(-4))
        actions.onKeyAction(KeyAction.Input("character-z", "Z"))
        assertEquals("xin Zchao", document.text)
    }

    @Test
    fun aVerticalMoveAsksTheEditorRatherThanRewritingTheSelection() {
        val document = GestureDocument("abcdefgh\nij\nklmnop")

        handler(document).onKeyAction(KeyAction.MoveCursor(columns = 0, lines = -1))

        // Only the editor knows where its text wraps, so the step goes out as arrow keys. This
        // fake ignores them, which is exactly why the caret and text are untouched here.
        assertEquals(2, document.keyEventCount)
        assertEquals("abcdefgh\nij\nklmnop", document.text)
        assertEquals(18, document.cursor)
    }

    @Test
    fun aZeroMoveWritesNothing() {
        val document = GestureDocument("xin chao")
        handler(document).onKeyAction(KeyAction.MoveCursor(0))
        assertEquals("xin chao", document.text)
        assertEquals(8, document.cursor)
    }
}
