package app.funput.funput.ime.shortcuts

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.CompositionRenderMode
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

/** Real EditText + JNI: successful editor calls must never erase the reopened engine. */
@RunWith(AndroidJUnit4::class)
class BackspaceShortcutInstrumentedTest {
    @Test fun repeatedBackspaceDeletesOneLetterAfterReopening() = onMainThread {
        for (method in KeyboardInputMethod.entries) {
            for (enabled in listOf(false, true)) {
                for (mode in listOf(CompositionRenderMode.COMPOSING, CompositionRenderMode.COMMITTED)) {
                    ImeEditingScenario.create(method, allowShortcuts = enabled).use { scenario ->
                        scenario.composition.setRenderMode(mode)
                        scenario.handler.receiveShortcuts(library())
                        scenario.handler.type(if (method == KeyboardInputMethod.VNI) "xin chao2 " else "xin chaof ")
                        assertEquals("xin chào ", scenario.text)
                        for (expected in listOf("xin chào", "xin chà", "xin ch", "xin c", "xin ")) {
                            scenario.handler.onKeyAction(KeyAction.Backspace)
                            assertEquals("$method / $enabled / $mode", expected, scenario.text)
                        }
                    }
                }
            }
        }
    }

    @Test fun reopenedWordCanBeRetonedAndThenBackspaced() = onMainThread {
        for (method in KeyboardInputMethod.entries) {
            ImeEditingScenario.create(method).use { scenario ->
                scenario.handler.type(if (method == KeyboardInputMethod.VNI) "chao2 " else "chaof ")
                scenario.handler.onKeyAction(KeyAction.Backspace)
                scenario.handler.type(if (method == KeyboardInputMethod.VNI) "1" else "s")
                assertEquals("cháo", scenario.text)
                scenario.handler.onKeyAction(KeyAction.Backspace)
                assertEquals("chá", scenario.text)
                scenario.handler.onKeyAction(KeyAction.Backspace)
                assertEquals("ch", scenario.text)
            }
        }
    }

    @Test fun correctedTriggersStillExpandInVietnameseAndEnglish() = onMainThread {
        for (language in KeyboardLanguage.entries) {
            ImeEditingScenario.create().use { scenario ->
                scenario.handler.receiveShortcuts(library())
                scenario.handler.onKeyAction(KeyAction.ToggleLanguage(language))
                scenario.handler.type("vnx")
                scenario.handler.onKeyAction(KeyAction.Backspace)
                scenario.handler.onKeyAction(KeyAction.Space)
                assertEquals("việt nam ", scenario.text)
            }
        }
    }

    @Test fun backspaceAfterExpansionKeepsTheLastVietnameseWord() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.type("vn ")
            for (expected in listOf("việt nam", "việt na", "việt n", "việt ")) {
                scenario.handler.onKeyAction(KeyAction.Backspace)
                assertEquals(expected, scenario.text)
            }
        }
    }

    @Test fun lateLibraryDoesNotChangeTheReopenedTriggerUntilItsBoundary() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(ShortcutLibrary(entries = listOf(
                TextShortcut(trigger = "chào", expansion = "old"),
            )))
            scenario.handler.onClipboardSelected("chào ")
            scenario.handler.onKeyAction(KeyAction.Backspace)
            assertEquals("chào", scenario.text)
            scenario.handler.receiveShortcuts(ShortcutLibrary(entries = listOf(
                TextShortcut(trigger = "chào", expansion = "new"),
            )))
            scenario.handler.onKeyAction(KeyAction.Space)
            assertEquals("old ", scenario.text)
            scenario.handler.onClipboardSelected("chào ")
            scenario.handler.onKeyAction(KeyAction.Backspace)
            scenario.handler.onKeyAction(KeyAction.Space)
            assertEquals("old new ", scenario.text)
        }
    }

    private fun library() = ShortcutLibrary(entries = listOf(TextShortcut(trigger = "vn", expansion = "việt nam")))
}
