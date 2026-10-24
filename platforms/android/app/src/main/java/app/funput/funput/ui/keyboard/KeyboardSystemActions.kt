package app.funput.funput.ui.keyboard

import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.net.toUri
import app.funput.funput.R
import app.funput.funput.ime.hardware.tile.HardwareKeyboardTileService
import app.funput.funput.ui.kit.icons.FunputIcons

internal fun Context.openKeyboardSettings() {
    startActivity(
        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

internal fun Context.openHardwareKeyboardSettings() {
    // Some OEM builds ship no physical-keyboard page; the keyboard list is the nearest one.
    try {
        startActivity(
            Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (missingPage: ActivityNotFoundException) {
        Log.w("Funput", "No physical keyboard settings page", missingPage)
        openKeyboardSettings()
    }
}

internal fun Context.requestAddHardwareKeyboardTile() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val statusBarManager = getSystemService(StatusBarManager::class.java)
        if (statusBarManager != null) {
            try {
                statusBarManager.requestAddTileService(
                    ComponentName(this, HardwareKeyboardTileService::class.java),
                    getString(app.funput.funput.ime.R.string.hardware_keyboard_tile_label),
                    Icon.createWithResource(this, FunputIcons.Keyboard),
                    mainExecutor,
                ) { result ->
                    when (result) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            showShortToast(R.string.settings_hardware_keyboard_tile_added)
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            showShortToast(R.string.settings_hardware_keyboard_tile_already_added)
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> Unit
                        else -> showShortToast(R.string.settings_hardware_keyboard_tile_manual_hint)
                    }
                }
                return
            } catch (unsupported: RuntimeException) {
                Log.w("Funput", "Cannot request Quick Settings tile placement", unsupported)
            }
        }
    }
    showShortToast(R.string.settings_hardware_keyboard_tile_manual_hint)
}

private fun Context.showShortToast(messageRes: Int) {
    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
}

internal fun Context.showKeyboardPicker() {
    getSystemService(InputMethodManager::class.java).showInputMethodPicker()
}

internal fun Context.openWebsite(url: String) {
    // A device with no browser, or no mail app for a mailto: link, is an ordinary device — not a
    // reason to take the app down.
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (missingHandler: ActivityNotFoundException) {
        Log.w("Funput", "No app handles $url", missingHandler)
    }
}
