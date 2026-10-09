package app.funput.funput.ime.extraonsets

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.localtext.LocalTextComposer
import app.funput.funput.ime.localtext.LocalTextSettings
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.FunputNative
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.ui.localtext.LocalTextEdit
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the packaged JNI symbols through the production Kotlin engine wrapper. */
@RunWith(AndroidJUnit4::class)
class ExtraOnsetsNativeInstrumentedTest {
    @Test fun eachLetterCanBeEnabledAndDisabledThroughConfigure() = onMainThread {
        NativeVietnameseEngine().use { engine ->
            val defaults = configuration()
            listOf(Triple("z", "zoo", "zô"), Triple("f", "fair", "fải"),
                Triple("w", "was", "wá"), Triple("j", "jowf", "jờ")).forEach { (letter, keys, expected) ->
                val selected = defaults.copy(extraOnsets = ExtraOnsetLetters.parse(letter))
                engine.configure(selected)
                assertEquals(selected, engine.configuration)
                assertEquals(expected, type(engine, keys))
                engine.configure(defaults)
                assertEquals(keys, type(engine, keys))
            }
        }
    }

    @Test fun vniAdvancedTelexAndEnglishRespectTheSelection() = onMainThread {
        NativeVietnameseEngine().use { engine ->
            val all = configuration().copy(extraOnsets = ExtraOnsetLetters.All)
            engine.configure(all.copy(inputMethod = KeyboardInputMethod.VNI))
            assertEquals("zô", type(engine, "zo6"))
            assertEquals("jờ", type(engine, "jo72"))
            engine.configure(all.copy(inputMethod = KeyboardInputMethod.TELEX_ADVANCED))
            assertEquals("ưa", type(engine, "wa"))
            assertEquals("wá", type(engine, "wwas"))
            engine.configure(all)
            assertEquals("food", type(engine, "food"))
            assertEquals("fát", type(engine, "fast"))
            assertEquals("fast", type(engine, "fasst"))
            engine.setEnabled(false)
            assertEquals("", type(engine, "zoo"))
            assertEquals("", engine.backspace())
            engine.setEnabled(true)
            assertEquals("zô", type(engine, "zoo"))
        }
    }

    @Test fun rawJniBitsMatchTheDocumentedLetterContract() = onMainThread {
        val handle = FunputNative.nativeCreate()
        try {
            FunputNative.nativeConfigure(handle, 0, 0, true, true, false, false)
            listOf(Triple(1, "fair", "fải"), Triple(2, "jowf", "jờ"),
                Triple(4, "was", "wá"), Triple(8, "zoo", "zô")).forEach { (bit, keys, expected) ->
                FunputNative.nativeClear(handle)
                FunputNative.nativeSetExtraOnsets(handle, bit or 0x100)
                var output = ""
                keys.forEach { output = FunputNative.nativeProcess(handle, it.code) }
                assertEquals(expected, output)
            }
        } finally {
            FunputNative.nativeDestroy(handle)
        }
        FunputNative.nativeSetExtraOnsets(handle, 15)
        FunputNative.nativeSetExtraOnsets(0, 15)
        FunputNative.nativeSetExtraOnsets(-1, 15)
    }

    @Test fun emojiFieldCopiesTheLatestSelectionOnReset() = onMainThread {
        var current = configuration().copy(extraOnsets = ExtraOnsetLetters.parse("z"))
        LocalTextComposer(NativeVietnameseEngine()) { LocalTextSettings(current, true) }.use { field ->
            field.reset()
            field.apply(LocalTextEdit.Text("zoo"))
            assertEquals("zô", field.text)
            current = current.copy(extraOnsets = ExtraOnsetLetters.None)
            field.reset()
            field.apply(LocalTextEdit.Text("zoo"))
            assertEquals("zoo", field.text)
        }
    }

    private fun configuration() = EngineConfiguration(
        inputMethod = KeyboardInputMethod.TELEX,
        toneStyle = ToneStyle.TRADITIONAL,
        smartRestore = true,
        eagerRestore = true,
        spellCheck = false,
    )

    private fun type(engine: NativeVietnameseEngine, keys: String): String {
        engine.clear()
        var output = ""
        keys.forEach { output = engine.process(it.code) }
        return output
    }
}
