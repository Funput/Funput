package app.funput.funput.ime.lifecycle

import android.inputmethodservice.InputMethodService
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.ImeSettingsController
import app.funput.funput.ime.hardware.HardwareKeyboard
import app.funput.funput.ime.shortcuts.ImeShortcutsController
import app.funput.funput.ime.shortcuts.createImeShortcutsController
import app.funput.funput.keyboard.model.ShiftState
import kotlinx.coroutines.CoroutineScope

internal class ImeRuntime(
    val session: ImeEditingSession,
    val settings: ImeSettingsController,
    val shortcuts: ImeShortcutsController,
    val hardwareKeyboard: HardwareKeyboard,
) {
    // Observe only after the owner stores this runtime: flows may emit synchronously.
    fun observe(service: InputMethodService, scope: CoroutineScope) {
        settings.observe(service, scope)
    }
}

internal fun createImeRuntime(
    service: InputMethodService,
    scope: CoroutineScope,
    views: ImeInputViewBinder,
    invalidateSpeech: () -> Unit,
): ImeRuntime {
    val session = createServiceEditingSession(service, scope) { views.view }
    val settings = createServiceSettings(session, { views.view }, invalidateSpeech, views::update)
    return ImeRuntime(
        session = session,
        settings = settings,
        shortcuts = createImeShortcutsController(service, scope, session.actionHandler),
        hardwareKeyboard = HardwareKeyboard.bind(service, session, scope) {
            views.view?.shiftState ?: ShiftState.OFF
        },
    )
}
