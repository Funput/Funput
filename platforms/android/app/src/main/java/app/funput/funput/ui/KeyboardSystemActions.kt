package app.funput.funput.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

internal fun Context.openKeyboardSettings() {
    startActivity(
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

internal fun Context.showKeyboardPicker() {
    getSystemService(InputMethodManager::class.java).showInputMethodPicker()
}
