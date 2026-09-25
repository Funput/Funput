package app.funput.funput.ime.hardware.visibility

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import app.funput.funput.ime.collectIn
import app.funput.funput.ime.settings.hardware.HardwareKeyboardSettings
import kotlinx.coroutines.CoroutineScope

/**
 * Applies [SoftKeyboardVisibilityState] to the running [InputMethodService].
 *
 * The service consults [showsWithHardwareKeyboard] from `onEvaluateInputViewShown()` and
 * `onShowInputRequested()`; this class moves the window when that answer changes.
 */
internal class SoftKeyboardVisibility(private val service: InputMethodService) {
    private val state = SoftKeyboardVisibilityState()

    val showsWithHardwareKeyboard: Boolean
        get() = state.showsWithHardwareKeyboard

    val hotkeyEnabled: Boolean
        get() = state.hotkeyEnabled

    fun observe(settings: HardwareKeyboardSettings, scope: CoroutineScope) {
        settings.preferences.collectIn(scope) { preferences ->
            state.update(preferences)
            // Android only re-asks onEvaluateInputViewShown() on its own events, so a setting
            // that takes the keyboard away has to close the window itself.
            if (service.isInputViewShown && !service.onEvaluateInputViewShown()) {
                service.requestHideSelf(0)
            }
        }
    }

    fun toggle() {
        when (state.toggle(currentlyShown = service.isInputViewShown)) {
            SoftKeyboardRequest.SHOW -> service.requestShowSelfCompat()
            SoftKeyboardRequest.HIDE -> service.requestHideSelf(0)
        }
    }

    fun onConfigurationChanged(config: Configuration) {
        if (!config.hasUsableHardwareKeyboard()) state.onKeyboardDetached()
    }
}

/** Mirrors the test `InputMethodService.onEvaluateInputViewShown()` applies by default. */
private fun Configuration.hasUsableHardwareKeyboard(): Boolean =
    keyboard != Configuration.KEYBOARD_NOKEYS &&
        hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES

/** `requestShowSelf` arrived in API 28; Funput's API 26 minimum needs the token-based call. */
private fun InputMethodService.requestShowSelfCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        requestShowSelf(0)
    } else {
        window?.window?.attributes?.token?.let(::legacyShowSelf)
    }
}

@Suppress("DEPRECATION")
private fun InputMethodService.legacyShowSelf(token: IBinder) =
    getSystemService(InputMethodManager::class.java).showSoftInputFromInputMethod(token, 0)
