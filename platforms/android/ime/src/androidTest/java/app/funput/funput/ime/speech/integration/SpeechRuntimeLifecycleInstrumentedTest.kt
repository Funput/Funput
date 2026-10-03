package app.funput.funput.ime.speech.integration

import android.text.InputType
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.KeyboardPanel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeechRuntimeLifecycleInstrumentedTest {
    @Test fun switchingPanelsCancelsBeforeAnyLaterFinal() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            val recording = f.start()
            f.keyboard.showSymbolsPanel()
            assertEquals(1, recording.cancels)
            assertEquals(KeyboardPanel.SYMBOLS, f.keyboard.activePanel)
            recording.emit(SpeechEvent.Final("obsolete"))
            assertEquals("", f.host.text)
            assertEquals(KeyboardPanel.SYMBOLS, f.keyboard.activePanel)
        }
    }

    @Test fun softKeyIsGuardedBeforeEditingAndLateFinalCannotAppend() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            val recording = f.start()
            f.keyboard.callbacks.onKeyAction!!.invoke(KeyAction.Input("a", "a"))
            assertEquals(1, recording.cancels)
            recording.emit(SpeechEvent.Final("obsolete"))
            assertEquals("a", f.host.text)
            assertEquals(KeyboardPanel.LETTERS, f.keyboard.activePanel)
        }
    }

    @Test fun everyEditorRestartInvalidatesEvenForTheSamePackageAndField() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            val recording = f.start()
            f.runtime.startInput(f.info)
            recording.emit(SpeechEvent.Final("obsolete"))
            assertEquals("", f.host.text)
            assertEquals(1, recording.cancels)
            assertEquals(KeyboardPanel.LETTERS, f.keyboard.activePanel)
        }
    }

    @Test fun hideAndSettingOffReleaseTheClientAndRejectLateCallbacks() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            val first = f.start()
            f.runtime.hide()
            first.emit(SpeechEvent.Final("hidden"))
            assertFalse(f.keyboard.microphone.visible)
            f.runtime.show()
            val second = f.start()
            f.runtime.setEnabled(false)
            second.emit(SpeechEvent.Final("disabled"))
            assertFalse(f.keyboard.microphone.visible)
            assertEquals("", f.host.text)
            assertEquals(1, first.closes)
            assertEquals(1, second.closes)
        }
    }

    @Test fun movingCaretAwayAndBackStillInvalidates() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            f.host.connection.commitText("ab", 1)
            f.runtime.selectionChanged(2, 2)
            val recording = f.start()
            f.host.moveCursorTo(0)
            f.runtime.selectionChanged(0, 0)
            f.host.moveCursorTo(2)
            f.runtime.selectionChanged(2, 2)
            recording.emit(SpeechEvent.Final("obsolete"))
            assertEquals("ab", f.host.text)
            assertEquals(1, recording.cancels)
        }
    }

    @Test fun excludedEditorsAndRangeSelectionsHideMicrophone() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            for (type in listOf(InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)) {
                f.info.inputType = type
                f.runtime.startInput(f.info)
                assertFalse(f.keyboard.microphone.visible)
            }
            f.info.inputType = InputType.TYPE_CLASS_TEXT
            f.info.initialSelEnd = 1
            f.runtime.startInput(f.info)
            assertFalse(f.keyboard.microphone.visible)
            assertTrue(f.backend.recordings.isEmpty())
        }
    }
}
