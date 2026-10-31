package app.funput.funput.ime.lifecycle

import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.ImeSettingsController
import app.funput.funput.keyboard.ui.FunputKeyboardView

internal fun createServiceSettings(
    session: ImeEditingSession,
    view: () -> FunputKeyboardView?,
    invalidateSpeech: () -> Unit,
    updateView: () -> Unit,
): ImeSettingsController = ImeSettingsController(
    // The composition's engine, not `session.nativeEngine`: the tone style must pass
    // through the same reporting wrapper as the VI/EN switch.
    engine = session.composition.engine,
    onInputMethodChanged = { method ->
        invalidateSpeech()
        session.restartComposition(method, view())
    },
    onViewSettingsChanged = {
        invalidateSpeech()
        updateView()
    },
    onPersonalSuggestionsChanged = { preferences ->
        session.suggestionService.configure(preferences)
        session.typo.wordSource.applyResetToken(preferences.resetToken)
    },
    onAutoCapitalizeChanged = session.editorRuntime::setAutoCapitalizeEnabled,
)
