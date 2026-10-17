package app.funput.funput.ime.correction

import android.view.inputmethod.InputConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.correction.TypoCorrection
import app.funput.funput.ime.editing.support.HostEditor
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.nativebridge.PersonalSuggestionNative
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.suggestions.correction.CorrectionDictionary
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.touch.KeyTouchAlternate
import app.funput.funput.keyboard.touch.KeyTouchEvidence
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real engine through JNI: a finger that lands on `h` while aiming for `g` ends the word
 * `đường`, the way the iOS UI test types it. Eager restore stays off, as in the iOS harness,
 * so the dead `đườnh` reaches Space as a syllable rather than as raw keys.
 */
@RunWith(AndroidJUnit4::class)
class TypoCorrectionInstrumentedTest {
    private object Unknown : CorrectionDictionary {
        override fun recognizes(word: String) = false
        override fun keep(word: String) = Unit
        override fun weight(word: String) = PersonalSuggestionNative.nativeSyllableWeight(word)
    }

    private class Typist(method: KeyboardInputMethod) : AutoCloseable {
        val host = HostEditor(ApplicationProvider.getApplicationContext())
        private val engine = NativeVietnameseEngine().apply {
            configure(EngineConfiguration(method, ToneStyle.TRADITIONAL, smartRestore = true,
                eagerRestore = false, spellCheck = true, typoCorrection = true))
        }
        private val corrections = TypoCorrection(engine.correction, enabled = { true }).apply { dictionary = Unknown }
        private val handler = ImeKeyActionHandler(
            composition = AndroidCompositionSession(engine, corrections = corrections),
            editor = InputConnectionEditor(),
            connection = { host.connection as InputConnection? },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        ).apply { start() }

        /** Every key dead centre except a slip on [slipped], which landed nearer [aimed]. */
        fun type(keys: String, slipped: Char, aimed: Char) = keys.forEach { key ->
            val touch = if (key == slipped) {
                KeyTouchEvidence(key.code, 0.62f, listOf(KeyTouchAlternate(aimed.code, 0.4f)))
            } else {
                KeyTouchEvidence(key.code, 0.05f)
            }
            handler.onKeyAction(KeyAction.Input("key-$key", key.toString(), touch))
        }

        fun space() = handler.onKeyAction(KeyAction.Space)
        fun backspace() = handler.onKeyAction(KeyAction.Backspace)

        override fun close() {
            handler.finish()
            engine.close()
        }
    }

    @Test
    fun vniSlipIsRepairedAtSpaceAndBackspaceTakesItBack() = onMainThread {
        Typist(KeyboardInputMethod.VNI).use { typist ->
            typist.type("d9u7o7nh2", slipped = 'h', aimed = 'g')
            // With spell check on, the tone digit cannot land on a dead syllable and stays.
            val typed = typist.host.text
            typist.space()
            assertEquals("đường ", typist.host.text)
            typist.backspace()
            assertEquals("Backspace puts back exactly what Space replaced", "$typed ", typist.host.text)
        }
    }

    @Test
    fun telexSlipIsRepairedAtSpace() = onMainThread {
        Typist(KeyboardInputMethod.TELEX).use { typist ->
            typist.type("dduwowfnh", slipped = 'h', aimed = 'g')
            typist.space()
            assertEquals("đường ", typist.host.text)
        }
    }

    @Test
    fun aWordTypedRightIsLeftAlone() = onMainThread {
        Typist(KeyboardInputMethod.VNI).use { typist ->
            typist.type("d9u7o7ng2", slipped = 'x', aimed = 'x')
            typist.space()
            assertEquals("đường ", typist.host.text)
        }
    }
}
