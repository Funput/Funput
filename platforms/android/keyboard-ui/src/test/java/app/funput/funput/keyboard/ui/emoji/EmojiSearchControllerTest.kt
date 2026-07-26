package app.funput.funput.keyboard.ui.emoji

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
}
