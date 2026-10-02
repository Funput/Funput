package app.funput.funput.ui.settings.extraonsets

import androidx.compose.runtime.Immutable
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Persisted selection and actions for the extra-onset section. */
@Immutable
internal class ExtraOnsetsSectionState(
    val selection: ExtraOnsetLetters,
    val inputMethod: KeyboardInputMethod,
    val isSaving: Boolean = false,
    val hasSaveError: Boolean = false,
    val onEnabledChanged: (Boolean) -> Unit = {},
    val onLetterChanged: (ExtraOnsetLetter, Boolean) -> Unit = { _, _ -> },
) {
    val showsAdvancedWHint: Boolean
        get() = inputMethod == KeyboardInputMethod.TELEX_ADVANCED && ExtraOnsetLetter.W in selection
}
