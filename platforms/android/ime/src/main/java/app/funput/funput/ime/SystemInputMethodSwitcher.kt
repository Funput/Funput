package app.funput.funput.ime

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.IBinder
import android.view.inputmethod.InputMethodManager

/** Bridges the API 28 service helpers while preserving Funput's API 26 minimum. */
internal class SystemInputMethodSwitcher(private val service: InputMethodService) {
    private val manager: InputMethodManager
        get() = service.getSystemService(InputMethodManager::class.java)

    fun isAvailable(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        service.shouldOfferSwitchingToNextInputMethod()
    } else {
        token()?.let(::legacyIsAvailable) == true
    }

    fun switch(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        service.switchToNextInputMethod(false)
    } else {
        token()?.let(::legacySwitch) == true
    }

    private fun token(): IBinder? = service.window?.window?.attributes?.token

    @Suppress("DEPRECATION")
    private fun legacyIsAvailable(token: IBinder): Boolean =
        manager.shouldOfferSwitchingToNextInputMethod(token)

    @Suppress("DEPRECATION")
    private fun legacySwitch(token: IBinder): Boolean =
        manager.switchToNextInputMethod(token, false)
}
