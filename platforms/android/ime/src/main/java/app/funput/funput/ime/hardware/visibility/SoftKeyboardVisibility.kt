package app.funput.funput.ime.hardware.visibility

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import app.funput.funput.ime.collectIn
import app.funput.funput.ime.hardware.tile.HardwareKeyboardTileSync
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
            syncServiceWindow()
        }
        HardwareKeyboardTileSync.requests.collectIn(scope) {
            syncServiceWindow()
        }
    }

    fun toggle() {
        applyRequest(state.toggle(currentlyShown = service.isInputViewShown))
    }

    fun onConfigurationChanged(config: Configuration) {
        if (!config.hasUsableHardwareKeyboard()) state.onKeyboardDetached()
    }

    private fun syncServiceWindow() {
        // Android only re-asks onEvaluateInputViewShown() on its own events, so a setting or
        // Quick Settings tile change has to open or close the window itself.
        state.syncWindow(
            currentlyShown = service.isInputViewShown,
            inputStarted = service.isInputStarted,
            shouldShow = service.onEvaluateInputViewShown(),
        )?.let(::applyRequest)
    }

    private fun applyRequest(request: SoftKeyboardRequest) {
        when (request) {
            SoftKeyboardRequest.SHOW -> service.requestShowSelfCompat()
            SoftKeyboardRequest.HIDE -> service.requestHideSelf(0)
        }
    }
}

private val InputMethodService.isInputStarted: Boolean
    get() = currentInputStarted && currentInputConnection != null

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
