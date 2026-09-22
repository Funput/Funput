package app.funput.funput.ime.shortcuts

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.composition.CompositionBoundary
import app.funput.funput.shortcuts.model.ShortcutLibrary

/** Couples async library installation to the raw word currently being typed. */
internal class ImeShortcutCoordinator(
    private val composition: AndroidCompositionSession,
    private val session: ImeShortcutSession,
    private val connection: () -> InputConnection?,
) {
    private val word = ShortcutWordTracker()
    val runsInEnglish: Boolean get() = session.runsInEnglish

    fun beginActivation() {
        word.clear()
        session.beginActivation()
    }

    fun receive(library: ShortcutLibrary) =
        session.receive(library, word.isActive || composition.isComposing)

    fun finish() {
        word.clear()
        session.finishWord()
    }

    fun backspace(tracksEnglish: Boolean) {
        if (tracksEnglish) session.backspaceEnglish()
        // Reopening restores the shared engine, but not this raw-keystroke tracker.
        // An empty tracker must not clear a Vietnamese composition that still owns it.
        if (word.backspace() && !composition.isComposing) session.finishWord()
    }

    fun track(text: String, vietnamese: Boolean) {
        val boundary = word.input(text) { codePoint ->
            if (vietnamese) composition.isBoundary(codePoint)
            else CompositionBoundary.isBoundary(codePoint)
        }
        if (boundary) session.finishWord()
    }

    fun reconcile() {
        if (word.isActive && !word.reconcile(connection())) session.finishWord()
    }
}
