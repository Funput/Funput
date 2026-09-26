package app.funput.funput.keyboard.ui.emoji.search

import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.ui.localtext.LocalTextComposing
import app.funput.funput.keyboard.ui.localtext.LocalTextEdit
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiSearchControllerTest {
    @Test fun `query stays inside pure search state`() {
        val states = mutableListOf<EmojiSearchState>()
        val controller = EmojiSearchController(states::add)
        controller.begin()
        controller.input("c")
        controller.space()
        controller.input("a")
        controller.backspace()
        controller.done()

        assertEquals(EmojiSearchMode.SHOWING_RESULTS, controller.state.mode)
        assertEquals("c ", controller.state.query)
    }

    @Test fun `clear preserves editing while cancel resets browser`() {
        val controller = EmojiSearchController {}
        controller.begin()
        controller.input("smile")
        controller.clear()
        assertEquals(EmojiSearchState(EmojiSearchMode.EDITING, ""), controller.state)
        controller.cancel()
        assertEquals(EmojiSearchState(), controller.state)
    }

    @Test fun `keys become field edits and the query mirrors the field`() {
        val field = RecordingField("cười")
        val controller = EmojiSearchController {}
        controller.field = { field }
        controller.begin()
        controller.input("7")
        controller.space()
        controller.backspace()
        assertEquals(
            listOf(LocalTextEdit.Text("7"), LocalTextEdit.Space, LocalTextEdit.DeleteBackward),
            field.edits,
        )
        assertEquals("cười", controller.state.query)
        assertEquals(KeyboardInputMethod.VNI, controller.state.inputMethod)
    }

    @Test fun `a search from the browser starts from a fresh field`() {
        val field = RecordingField()
        val controller = EmojiSearchController {}
        controller.field = { field }
        controller.begin()
        controller.done()
        controller.begin()
        assertEquals(1, field.resets)
        controller.clear()
        controller.cancel()
        assertEquals(3, field.resets)
    }

    private class RecordingField(override val text: String = "") : LocalTextComposing {
        override val inputMethod = KeyboardInputMethod.VNI
        val edits = mutableListOf<LocalTextEdit>()
        var resets = 0

        override fun apply(edit: LocalTextEdit) {
            edits += edit
        }

        override fun reset() {
            resets++
        }
    }
}
