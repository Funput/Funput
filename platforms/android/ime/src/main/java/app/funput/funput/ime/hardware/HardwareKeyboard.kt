package app.funput.funput.ime.hardware

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.hardware.visibility.SoftKeyboardVisibility
import app.funput.funput.ime.settings.hardware.HardwareKeyboardSettings
import app.funput.funput.keyboard.model.ShiftState
import kotlinx.coroutines.CoroutineScope

/**
 * Everything a physical keyboard changes about the IME: the keys it types, the Alt+Space
 * hotkey, and whether the soft keyboard stays on screen beside it.
 */
internal class HardwareKeyboard(
    private val keys: ImeHardwareKeyHandler,
    private val softKeyboard: SoftKeyboardVisibility,
) {
    /** True when Funput should show although Android's default would hide it. */
    val showsSoftKeyboard: Boolean
        get() = softKeyboard.showsWithHardwareKeyboard

    fun onKeyDown(event: KeyEvent): Boolean = keys.onKeyDown(event.toHardwareKeyStroke())

    fun onKeyUp(event: KeyEvent): Boolean = keys.onKeyUp(event.toHardwareKeyStroke())

    fun onConfigurationChanged(config: Configuration) = softKeyboard.onConfigurationChanged(config)

    companion object {
        fun bind(
            service: InputMethodService,
            session: ImeEditingSession,
            scope: CoroutineScope,
            currentShift: () -> ShiftState,
        ): HardwareKeyboard {
            val softKeyboard = SoftKeyboardVisibility(service)
            softKeyboard.observe(HardwareKeyboardSettings(service), scope)
            return HardwareKeyboard(ImeHardwareKeyHandler.bind(session, softKeyboard, currentShift), softKeyboard)
        }
    }
}
