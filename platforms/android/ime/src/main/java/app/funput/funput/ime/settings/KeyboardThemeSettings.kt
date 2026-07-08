package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.theme.KeyboardThemeId
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists keyboard theme presets shared by the settings app and IME. */
class KeyboardThemeSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val themeId: Flow<KeyboardThemeId> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> KeyboardThemeSettingCodec.decode(preferences[ThemeIdKey]) }
        .distinctUntilChanged()

    suspend fun setTheme(themeId: KeyboardThemeId) {
        dataStore.edit { preferences ->
            preferences[ThemeIdKey] = KeyboardThemeSettingCodec.encode(themeId)
        }
    }

    companion object {
        val DefaultThemeId = KeyboardThemeId.Default

        private val ThemeIdKey = stringPreferencesKey("keyboard_theme_id")
    }
}

internal object KeyboardThemeSettingCodec {
    fun encode(themeId: KeyboardThemeId): String = themeId.value

    fun decode(value: String?): KeyboardThemeId =
        KeyboardThemeId.parseOrNull(value) ?: KeyboardThemeId.Default
}
