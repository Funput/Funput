package app.funput.funput.ime.settings.hardware

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.funput.funput.ime.settings.funputSettingsStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** How Funput behaves while a physical keyboard is attached. */
data class HardwareKeyboardPreferences(
    /** Keep the soft keyboard on screen instead of Android's default of hiding it. */
    val showsSoftKeyboard: Boolean,
    /** Alt+Space shows or hides the soft keyboard; off leaves Alt+Space typing a space. */
    val toggleHotkeyEnabled: Boolean,
) {
    companion object {
        val Default = HardwareKeyboardPreferences(
            showsSoftKeyboard = false,
            toggleHotkeyEnabled = true,
        )
    }
}

/** Persists [HardwareKeyboardPreferences], shared by Settings and the IME. */
class HardwareKeyboardSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val preferences: Flow<HardwareKeyboardPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            HardwareKeyboardSettingCodec.decode(
                showsSoftKeyboard = values[ShowsSoftKeyboardKey],
                toggleHotkeyEnabled = values[ToggleHotkeyEnabledKey],
            )
        }
        .distinctUntilChanged()

    suspend fun setShowsSoftKeyboard(enabled: Boolean) {
        dataStore.edit { it[ShowsSoftKeyboardKey] = enabled }
    }

    suspend fun setToggleHotkeyEnabled(enabled: Boolean) {
        dataStore.edit { it[ToggleHotkeyEnabledKey] = enabled }
    }

    private companion object {
        val ShowsSoftKeyboardKey = booleanPreferencesKey("hardware_keyboard_shows_soft_keyboard")
        val ToggleHotkeyEnabledKey = booleanPreferencesKey("hardware_keyboard_toggle_hotkey_enabled")
    }
}

internal object HardwareKeyboardSettingCodec {
    fun decode(
        showsSoftKeyboard: Boolean?,
        toggleHotkeyEnabled: Boolean?,
    ) = HardwareKeyboardPreferences(
        showsSoftKeyboard = showsSoftKeyboard ?: HardwareKeyboardPreferences.Default.showsSoftKeyboard,
        toggleHotkeyEnabled = toggleHotkeyEnabled
            ?: HardwareKeyboardPreferences.Default.toggleHotkeyEnabled,
    )
}
