package app.funput.funput.ime.shortcuts

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.ime.shortcuts.undo.ShortcutUndo
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Real EditText + JNI: one Backspace puts a gõ tắt expansion back. */
@RunWith(AndroidJUnit4::class)
class ShortcutUndoInstrumentedTest {
    @Test fun theChipNamesTheTriggerUntilBackspaceTakesIt() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.type("vn ")
            val undo = ShortcutUndo(scenario.composition) { scenario.host.connection }

            assertEquals("việt nam ", scenario.text)
            assertEquals("vn", undo.chipText())
            scenario.handler.onKeyAction(KeyAction.Backspace)
            assertEquals("vn ", scenario.text)
            assertNull(undo.chipText())
        }
    }

    @Test fun englishModeRestoresTheTriggerAndKeepsThePeriod() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.onKeyAction(KeyAction.ToggleLanguage(KeyboardLanguage.ENGLISH))
            scenario.handler.type("vn.")
            val undo = ShortcutUndo(scenario.composition) { scenario.host.connection }

            assertEquals("việt nam.", scenario.text)
            assertEquals("vn", undo.chipText())
            scenario.handler.onKeyAction(KeyAction.Backspace)
            assertEquals("vn.", scenario.text)
        }
    }

    @Test fun theNextLetterSpendsTheUndo() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.type("vn x")
            scenario.handler.onKeyAction(KeyAction.Backspace)
            assertEquals("việt nam ", scenario.text)
        }
    }

    @Test fun theRefusedTriggerIsKeptOnce() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library())
            scenario.handler.type("vn ")
            scenario.handler.onKeyAction(KeyAction.Backspace)
            scenario.handler.type("vn ")
            assertEquals("vn vn ", scenario.text)
            scenario.handler.type("vn ")
            assertEquals("vn vn việt nam ", scenario.text)
        }
    }

    @Test fun anotherTriggerStillExpandsWhileOneIsRefused() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.receiveShortcuts(library(TextShortcut(trigger = "kg", expansion = "không")))
            scenario.handler.type("vn ")
            scenario.handler.onKeyAction(KeyAction.Backspace)
            scenario.handler.type("kg ")
            assertEquals("vn không ", scenario.text)
        }
    }

    private fun library(extra: TextShortcut? = null) = ShortcutLibrary(
        entries = listOfNotNull(TextShortcut(trigger = "vn", expansion = "việt nam"), extra),
    )
}
