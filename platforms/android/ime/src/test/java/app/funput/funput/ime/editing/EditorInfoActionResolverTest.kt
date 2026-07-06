package app.funput.funput.ime.editing

import android.view.inputmethod.EditorInfo
import app.funput.funput.keyboard.model.KeyboardEnterAction
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorInfoActionResolverTest {
    @Test
    fun `standard actions resolve to matching presentation and command`() {
        val cases = mapOf(
            EditorInfo.IME_ACTION_GO to KeyboardEnterAction.Standard.GO,
            EditorInfo.IME_ACTION_SEARCH to KeyboardEnterAction.Standard.SEARCH,
            EditorInfo.IME_ACTION_SEND to KeyboardEnterAction.Standard.SEND,
            EditorInfo.IME_ACTION_NEXT to KeyboardEnterAction.Standard.NEXT,
            EditorInfo.IME_ACTION_DONE to KeyboardEnterAction.Standard.DONE,
            EditorInfo.IME_ACTION_PREVIOUS to KeyboardEnterAction.Standard.PREVIOUS,
        )

        cases.forEach { (actionId, presentation) ->
            assertEquals(
                ImeEditorAction(presentation, ImeEditCommand.PerformEditorAction(actionId)),
                EditorInfoActionResolver.resolve(actionId),
            )
        }
    }

    @Test
    fun `non action flags do not hide the masked action`() {
        val options = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_FULLSCREEN

        assertEquals(
            KeyboardEnterAction.Standard.SEARCH,
            EditorInfoActionResolver.resolve(options).presentation,
        )
    }

    @Test
    fun `none and unspecified actions keep the new line key`() {
        assertEquals(ImeEditorAction.NewLine, EditorInfoActionResolver.resolve(EditorInfo.IME_ACTION_NONE))
        assertEquals(ImeEditorAction.NewLine, EditorInfoActionResolver.resolve(EditorInfo.IME_ACTION_UNSPECIFIED))
    }

    @Test
    fun `no enter action flag preserves new line behavior`() {
        val options = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION

        assertEquals(ImeEditorAction.NewLine, EditorInfoActionResolver.resolve(options, "Send now", 99))
    }

    @Test
    fun `custom action uses application label and id`() {
        assertEquals(
            ImeEditorAction(
                KeyboardEnterAction.Custom("Apply"),
                ImeEditCommand.PerformEditorAction(73),
            ),
            EditorInfoActionResolver.resolve(EditorInfo.IME_ACTION_UNSPECIFIED, "  Apply  ", 73),
        )
    }
}
