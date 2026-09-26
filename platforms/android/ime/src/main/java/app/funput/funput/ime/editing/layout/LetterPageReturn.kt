package app.funput.funput.ime.editing.layout

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.collectIn
import app.funput.funput.ime.settings.layout.LetterPageReturnSettings
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.KeyboardPanel
import kotlinx.coroutines.CoroutineScope

/**
 * Brings back the letters when a space follows punctuation typed on the symbol panel, so a
 * sentence can go on in Vietnamese without reaching for "ABC".
 *
 * The keyboard view knows the panel and the editor knows the text, so the decision sits
 * where a key action passes between them. The order is the point: the text is read before
 * the space is committed, while the punctuation is still the last character, and the panel
 * changes only after it, so the capitalization that follows the panel change sees `"! "`.
 */
internal class LetterPageReturn(private val connection: () -> InputConnection?) {
    var enabled = LetterPageReturnSettings.DefaultEnabled

    fun observe(settings: LetterPageReturnSettings, scope: CoroutineScope) {
        settings.enabled.collectIn(scope) { enabled = it }
    }

    /** Commits [action] through [commit], then calls [showLetters] if the rule applies. */
    fun dispatch(
        action: KeyAction,
        panel: KeyboardPanel,
        commit: () -> Unit,
        showLetters: () -> Unit,
    ) {
        val returns = action == KeyAction.Space && panel == KeyboardPanel.SYMBOLS && followsPunctuation()
        commit()
        if (returns) showLetters()
    }

    fun dispatch(action: KeyAction, view: FunputKeyboardView, commit: () -> Unit) =
        dispatch(action, view.activePanel, commit, view::showLettersPanel)

    private fun followsPunctuation(): Boolean {
        if (!enabled) return false
        val current = connection() ?: return false
        if (!current.getSelectedText(0).isNullOrEmpty()) return false
        return LetterPageReturnRule.appliesTo(current.getTextBeforeCursor(Lookback, 0))
    }

    private companion object {
        /** Every trigger is a single UTF-16 unit, and the rule reads only the last one. */
        const val Lookback = 1
    }
}
