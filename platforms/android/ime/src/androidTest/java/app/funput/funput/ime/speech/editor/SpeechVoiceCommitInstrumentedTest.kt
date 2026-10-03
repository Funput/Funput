package app.funput.funput.ime.speech.editor

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.ime.speech.session.SpeechEditorAnchor
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real editor + JNI; synthetic text verifies insertion semantics without recording audio. */
@RunWith(AndroidJUnit4::class)
class SpeechVoiceCommitInstrumentedTest {
    @Test fun finalBypassesTelexAndVniThenOrdinaryTypingContinues() = onMainThread {
        for (method in listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.VNI)) {
            ImeEditingScenario.create(method).use { scenario ->
                scenario.handler.type(if (method == KeyboardInputMethod.VNI) "a1" else "as")
                scenario.handler.finish()
                val tracker = SpeechEditorTracker().apply {
                    startInput(true, scenario.host.editText.selectionStart, scenario.host.editText.selectionEnd)
                }
                var anchor: SpeechEditorAnchor? = null
                val resolver = SpeechAnchorResolver(tracker, object : SpeechAnchorEnvironment {
                    override fun available() = true
                    override fun finishComposition() = scenario.handler.finish()
                    override fun selection(): SpeechSelection? = null // No fresh callback required.
                })
                resolver.prepare { anchor = it }
                assertNotNull(anchor)
                assertTrue(tracker.matches(anchor!!))
                assertTrue(scenario.handler.commitVoiceText("aa dd ww a1🙂!"))
                assertEquals("áaa dd ww a1🙂!", scenario.text)
                assertEquals(AuthoredSuggestionUpdate.Empty, scenario.handler.takeSuggestionUpdate())
                assertFalse(scenario.composition.isComposing)
                scenario.handler.type(if (method == KeyboardInputMethod.VNI) " a1" else " as")
                assertEquals("áaa dd ww a1🙂! á", scenario.text)
            }
        }
    }

    @Test fun compositionFinishCanReconcileTheLiveSelectionWithoutACallback() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")
            val tracker = SpeechEditorTracker().apply {
                startInput(true, scenario.host.editText.selectionStart, scenario.host.editText.selectionEnd)
                invalidate(expectSelection = true)
            }
            var anchor: SpeechEditorAnchor? = null
            SpeechAnchorResolver(tracker, object : SpeechAnchorEnvironment {
                override fun available() = true
                override fun finishComposition() = scenario.handler.finish()
                override fun selection() = SpeechSelection(
                    scenario.host.editText.selectionStart, scenario.host.editText.selectionEnd)
            }).prepare { anchor = it }
            assertEquals(4, anchor!!.caret)
            assertFalse(scenario.composition.isComposing)
            assertTrue(scenario.handler.commitVoiceText("Xin CHÀO,  bạn!"))
            assertEquals("việtXin CHÀO,  bạn!", scenario.text)
            assertEquals(AuthoredSuggestionUpdate.Empty, scenario.handler.takeSuggestionUpdate())
        }
    }
}
