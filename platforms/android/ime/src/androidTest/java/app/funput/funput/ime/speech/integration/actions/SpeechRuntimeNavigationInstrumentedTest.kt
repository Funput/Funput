package app.funput.funput.ime.speech.integration.actions

import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnectionWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.speech.integration.SpeechRuntimeFixture
import app.funput.funput.ime.speech.model.SpeechEvent
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.speech.cancelSpeechPanel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeechRuntimeNavigationInstrumentedTest {
    @Test fun cancelAndShiftCanStartAgainWithoutExtractedTextOrANewSelectionCallback() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            withoutExtractedText(f)
            val first = f.start()
            assertTrue(f.keyboard.cancelSpeechPanel()) // Same navigation path as the panel's Cancel button.
            first.emit(SpeechEvent.Final("obsolete"))
            assertEquals("", f.host.text)
            f.start()
            assertEquals(2, f.backend.recordings.size)
            assertEquals(1, first.cancels)
            f.runtime.back()
            f.keyboard.callbacks.onKeyAction!!.invoke(KeyAction.Shift(ShiftState.ON))
            f.start()
            assertEquals(3, f.backend.recordings.size)
        }
    }

    @Test fun symbolsAndPlacementNavigationDoNotRequireANewCaretConfirmation() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            withoutExtractedText(f)
            f.keyboard.showSymbolsPanel()
            f.keyboard.showLettersPanel()
            f.keyboard.callbacks.onPlacementEditorRequested!!.invoke()
            f.keyboard.callbacks.onPlacementChanged!!.invoke(f.keyboard.placementPreferences)
            f.start()
            assertEquals(1, f.backend.recordings.size)
        }
    }

    @Test fun navigationDoesNotClearAnActualEditWaitingForSelection() = onMainThread {
        SpeechRuntimeFixture().use { f ->
            withoutExtractedText(f)
            f.keyboard.callbacks.onKeyAction!!.invoke(KeyAction.Input("a", "a"))
            f.keyboard.callbacks.onKeyAction!!.invoke(KeyAction.Shift(ShiftState.ON))
            f.keyboard.showSymbolsPanel()
            f.keyboard.showLettersPanel()
            f.keyboard.callbacks.onSpeechRequested!!.invoke()
            assertTrue(f.backend.recordings.isEmpty())
            f.runtime.selectionChanged(1, 1)
            assertEquals(1, f.backend.recordings.size)
            f.backend.recordings.single().emit(SpeechEvent.Final("voice"))
            assertEquals("avoice", f.host.text)
        }
    }

    private fun withoutExtractedText(f: SpeechRuntimeFixture) {
        f.service.connection = object : InputConnectionWrapper(f.host.connection, false) {
            override fun getExtractedText(request: ExtractedTextRequest?, flags: Int) = null
        }
    }
}
