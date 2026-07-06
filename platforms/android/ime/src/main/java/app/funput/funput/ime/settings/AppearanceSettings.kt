package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists appearance choices shared by the settings app and keyboard. */
class AppearanceSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val mode: Flow<AppearanceMode> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> AppearanceSettingCodec.decode(preferences[AppearanceModeKey]) }
        .distinctUntilChanged()

    suspend fun setMode(mode: AppearanceMode) {
        dataStore.edit { preferences ->
            preferences[AppearanceModeKey] = AppearanceSettingCodec.encode(mode)
        }
    }

    companion object {
        val DefaultMode = AppearanceMode.SYSTEM

        private val AppearanceModeKey = stringPreferencesKey("appearance_mode")
    }
}

internal object AppearanceSettingCodec {
    fun encode(mode: AppearanceMode): String = mode.name

    fun decode(value: String?): AppearanceMode =
        AppearanceMode.entries.firstOrNull { it.name == value }
            ?: AppearanceSettings.DefaultMode
}
