package app.funput.funput.ui.settings.extraonsets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetsSettings
import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Shares Settings' existing composition snapshot without adding another collector. */
@Composable
internal fun rememberExtraOnsetsSectionState(
    selection: ExtraOnsetLetters,
    inputMethod: KeyboardInputMethod,
): ExtraOnsetsSectionState {
    val context = LocalContext.current
    val store = remember(context) { ExtraOnsetsSettings(context) }
    val scope = rememberCoroutineScope()
    val model = remember(store, scope) { ExtraOnsetsSectionModel(store, scope) }
    return ExtraOnsetsSectionState(
        selection = selection,
        inputMethod = inputMethod,
        isSaving = model.isSaving,
        hasSaveError = model.hasSaveError,
        onEnabledChanged = model::setEnabled,
        onLetterChanged = model::setLetter,
    )
}
