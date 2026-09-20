package app.funput.funput.ime.shortcuts

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises Android editing, JNI and the shared Rust matcher as one pipeline. */
@RunWith(AndroidJUnit4::class)
class ImeShortcutInstrumentedTest {
    @Test fun telexAndVniExpandOnBoundary() = onMainThread {
        listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.VNI,
            KeyboardInputMethod.TELEX_ADVANCED).forEach { method ->
            ImeEditingScenario.create(method).use { scenario ->
                scenario.handler.receiveShortcuts(library())
                scenario.handler.type("vn ")
                assertEquals("việt nam ", scenario.text)
            }
        }
    }

    @Test fun englishModeUsesRustSmartCaseWithoutComposing() = onMainThread {
        listOf("vn" to "việt nam.", "Vn" to "Việt Nam.", "VN" to "VIỆT NAM.")
            .forEach { (trigger, expected) ->
                ImeEditingScenario.create().use { scenario ->
                    scenario.handler.receiveShortcuts(library())
                    scenario.handler.onKeyAction(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
                    scenario.handler.type("$trigger.")
                    assertEquals(expected, scenario.text)
                }
            }
    }

    @Test fun unicodeMultilineExpansionCrossesJniUnchanged() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(ShortcutLibrary(entries = listOf(
                TextShortcut(trigger = "sig", expansion = "Xin chào 👋🏽\nFunput"),
            )))
            scenario.handler.type("sig ")
            assertEquals("Xin chào 👋🏽\nFunput ", scenario.text)
        }
    }

    @Test fun englishOptionAndProtectedEditorPreventExpansion() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library(inEnglish = false))
            scenario.handler.onKeyAction(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
            scenario.handler.type("vn ")
            assertEquals("vn ", scenario.text)
        }
        ImeEditingScenario.create(allowComposition = false).use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.type("vn ")
            assertEquals("vn ", scenario.text)
        }
        ImeEditingScenario.create(allowShortcuts = false).use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.type("vn ")
            assertEquals("vn ", scenario.text)
        }
    }

    @Test fun expansionDoesNotLeaveALearnedTrigger() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.onKeyAction(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
            scenario.handler.type("vn ")
            val update = scenario.handler.takeSuggestionUpdate()
            assertEquals("", update.prefix)
            assertEquals(null, update.completedToken)
        }
    }

    private fun library(inEnglish: Boolean = true) = ShortcutLibrary(
        entries = listOf(TextShortcut(trigger = "vn", expansion = "việt nam")),
        inEnglish = inEnglish,
    )
}
