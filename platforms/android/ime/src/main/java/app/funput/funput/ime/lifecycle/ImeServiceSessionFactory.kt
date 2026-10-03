package app.funput.funput.ime.lifecycle

import android.inputmethodservice.InputMethodService
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Keeps service callback wiring small enough to add the debug speech lifecycle safely. */
internal fun createServiceEditingSession(
    service: InputMethodService,
    scope: CoroutineScope,
    view: () -> FunputKeyboardView?,
): ImeEditingSession {
    lateinit var session: ImeEditingSession
    session = createImeEditingSession(
        context = service,
        scope = scope,
        editor = InputConnectionEditor(),
        connection = { service.currentInputConnection },
        currentShiftState = { view()?.shiftState ?: ShiftState.OFF },
        updateShiftState = { state -> view()?.shiftState = state },
        showSuggestions = { values -> view()?.suggestions = values },
        acknowledgeReset = { token ->
            scope.launch { session.suggestionSettings.acknowledgeReset(token) }
        },
    )
    return session
}
