package app.funput.funput.ui.keyboard

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
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

internal fun Context.openWebsite(url: String) {
    // A device with no browser, or no mail app for a mailto: link, is an ordinary device — not a
    // reason to take the app down.
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (missingHandler: ActivityNotFoundException) {
        Log.w("Funput", "No app handles $url", missingHandler)
    }
}
