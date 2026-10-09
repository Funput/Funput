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
    engine = session.nativeEngine,
    onInputMethodChanged = { method ->
        invalidateSpeech()
        session.restartComposition(method, view())
    },
    onViewSettingsChanged = {
        invalidateSpeech()
        updateView()
    },
    onPersonalSuggestionsChanged = session.suggestionService::configure,
    onAutoCapitalizeChanged = session.editorRuntime::setAutoCapitalizeEnabled,
)
