package app.funput.funput.ime.editing

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real engine + composition pipeline against a real EditText InputConnection. */
@RunWith(AndroidJUnit4::class)
class ImeCompositionInstrumentedTest {
    @Test
    fun telexComposesVietnameseWord() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")

            assertEquals("việt", scenario.text)
        }
    }

    @Test
    fun telexAppliesToneMark() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("as")

            assertEquals("á", scenario.text)
        }
    }

    @Test
    fun vniComposesAfterMethodSwitch() = onMainThread {
        ImeEditingScenario.create(KeyboardInputMethod.VNI).use { scenario ->
            scenario.handler.type("a1")

            assertEquals("á", scenario.text)
        }
    }

    @Test
    fun advancedTelexShortcutsComposeThroughJni() = onMainThread {
        val cases = listOf(
            "w" to "ư",
            "wf" to "ừ",
            "t[" to "tư",
            "m]" to "mơ",
            "tr[]ngf" to "trường",
            "ng[]if" to "người",
            "ww" to "w",
            "Wf" to "Ừ",
        )
        cases.forEach { (keys, expected) ->
            ImeEditingScenario.create(KeyboardInputMethod.TELEX_ADVANCED).use { scenario ->
                scenario.handler.type(keys)

                assertEquals("Failed input: $keys", expected, scenario.text)
            }
        }
    }

    @Test
    fun bracketsStayLiteralOutsideAdvancedTelex() = onMainThread {
        listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.VNI).forEach { method ->
            ImeEditingScenario.create(method).use { scenario ->
                scenario.handler.type("t[")

                assertEquals("Failed method: $method", "t[", scenario.text)
            }
        }
    }

    @Test
    fun bracesStayLiteralInAdvancedTelex() = onMainThread {
        ImeEditingScenario.create(KeyboardInputMethod.TELEX_ADVANCED).use { scenario ->
            scenario.handler.type("{}")

            assertEquals("{}", scenario.text)
        }
    }

    @Test
    fun kaomojiCommitsWholeAndVietnameseCompositionContinues() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")
            scenario.handler.onEmojiSelected(" ¯\\_(ツ)_/¯ ")
            scenario.handler.type("as")

            assertEquals("việt ¯\\_(ツ)_/¯ á", scenario.text)
            assertTrue(scenario.composition.isComposing)
        }
    }

    @Test
    fun clipboardCommitsWholeAndVietnameseCompositionContinues() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")
            scenario.handler.onClipboardSelected(" Việt,\\\nline ")
            scenario.handler.type("as")

            assertEquals("việt Việt,\\\nline á", scenario.text)
            assertTrue(scenario.composition.isComposing)
        }
    }

    @Test
    fun backspaceEditsCompositionNotHostBuffer() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")
            scenario.handler.onKeyAction(KeyAction.Backspace)

            assertEquals("việ", scenario.text)
            assertTrue(scenario.composition.isComposing)
        }
    }

    @Test
    fun cursorMoveOutsideCompositionFinalizesText() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")
            val composingEnd = scenario.host.editText.selectionEnd
            scenario.host.moveCursorTo(0)
            scenario.handler.onSelectionChanged(newStart = 0, newEnd = 0, composingEnd = composingEnd)

            assertFalse(scenario.composition.isComposing)
            assertEquals("việt", scenario.text)
        }
    }
}
