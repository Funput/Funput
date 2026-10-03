package app.funput.funput.ime.speech.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeechRuntimeDeliveryInstrumentedTest {
    @Test fun microphonePanelFinalAndTypingUseTheRealEditorPipeline() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            assertTrue(f.keyboard.microphone.visible)
            assertEquals(0, f.preparation.checks)
            f.editing.actionHandler.type("vieejt")
            val recording = f.start()
            assertEquals(KeyboardPanel.SPEECH, f.keyboard.activePanel)
            assertEquals(SpeechPanelStage.LISTENING, f.keyboard.speechPanelState.stage)
            assertEquals(0, recording.cancels) // Opening Speech must not invalidate the session.
            recording.emit(SpeechEvent.Partial("aa dd"))
            assertEquals("việt", f.host.text)
            f.keyboard.shiftState = ShiftState.CAPS_LOCK
            recording.emit(SpeechEvent.Final("  aa dd a1!  "))
            assertEquals("việtaa dd a1!", f.host.text)
            assertEquals(KeyboardPanel.LETTERS, f.keyboard.activePanel)
            assertEquals(ShiftState.OFF, f.keyboard.shiftState) // Sentence rules wait for a separator.
            assertEquals(AuthoredSuggestionUpdate.Empty, f.editing.actionHandler.takeSuggestionUpdate())
            recording.emit(SpeechEvent.Final("duplicate"))
            assertEquals("việtaa dd a1!", f.host.text)
            f.keyboard.callbacks.onKeyAction!!.invoke(KeyAction.Space)
            f.editing.actionHandler.type("as")
            assertEquals("việtaa dd a1! á", f.host.text)
            assertEquals(1, recording.closes)
        }
    }

    @Test fun stopKeepsPanelUntilFinalAndCancelDiscardsPreview() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            val recording = f.start()
            recording.emit(SpeechEvent.Partial("preview"))
            f.keyboard.callbacks.onSpeechAction!!.invoke(SpeechPanelAction.STOP)
            assertEquals(1, recording.stops)
            assertEquals(SpeechPanelStage.FINALIZING, f.keyboard.speechPanelState.stage)
            assertEquals(KeyboardPanel.SPEECH, f.keyboard.activePanel)
            f.keyboard.callbacks.onSpeechAction!!.invoke(SpeechPanelAction.CANCEL)
            assertEquals(KeyboardPanel.LETTERS, f.keyboard.activePanel)
            assertEquals("", f.keyboard.speechPanelState.preview)
            recording.emit(SpeechEvent.Final("cancelled"))
            assertEquals("", f.host.text)
            assertEquals(1, recording.cancels)
        }
    }

    @Test fun programmaticLettersAfterFinalDoesNotInvalidateTheCommit() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            val recording = f.start()
            recording.emit(SpeechEvent.Final("Xin chào."))
            assertEquals("Xin chào.", f.host.text)
            assertEquals(KeyboardPanel.LETTERS, f.keyboard.activePanel)
            assertEquals(0, recording.cancels)
            assertTrue(f.keyboard.microphone.visible)
        }
    }
}
