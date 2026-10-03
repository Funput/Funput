package app.funput.funput.ime.speech.integration.actions

import app.funput.funput.ime.speech.editor.SpeechEditorTracker
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.*
import org.junit.Test

class SpeechInputEditPolicyTest {
    @Test fun modifiersAndPanelNavigationKeepAnUnchangedCaretConfirmed() {
        val tracker = SpeechEditorTracker().apply { startInput(true, 3, 3) }
        val navigation = listOf(KeyAction.Shift(ShiftState.ON), KeyAction.Symbols,
            KeyAction.MoreSymbols, KeyAction.Letters, KeyAction.SwitchInputMethod)
        for (action in navigation) {
            tracker.invalidate(SpeechInputEditPolicy.expectsSelection(action))
            assertFalse(tracker.selectionPending)
            assertTrue(tracker.matches(tracker.anchor()!!))
        }
    }

    @Test fun textAndCaretCommandsWaitForConfirmationEvenIfNavigationFollows() {
        val edits = listOf(KeyAction.Input("a", "a"), KeyAction.Space, KeyAction.Backspace,
            KeyAction.Enter, KeyAction.DeleteWord, KeyAction.MoveCursor(1),
            KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
        for (action in edits) {
            val tracker = SpeechEditorTracker().apply { startInput(true, 3, 3) }
            tracker.invalidate(SpeechInputEditPolicy.expectsSelection(action))
            tracker.invalidate(SpeechInputEditPolicy.expectsSelection(KeyAction.Shift(ShiftState.ON)))
            assertTrue(tracker.selectionPending)
            assertFalse(tracker.matches(tracker.anchor()!!))
            tracker.selectionChanged(3, 3)
            assertTrue(tracker.matches(tracker.anchor()!!))
        }
    }
}
