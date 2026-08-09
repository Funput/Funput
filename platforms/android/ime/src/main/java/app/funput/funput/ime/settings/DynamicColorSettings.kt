package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Persists whether the settings app tints itself from the system wallpaper palette.
 *
 * This only affects the app UI. Keyboard colours come from the keyboard theme the user picked in
 * the gallery, which is a separate setting on purpose: the keyboard has to stay legible against
 * whatever app is hosting it. The store lives here because [funputSettingsStore] does.
 */
class DynamicColorSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val enabled: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> DynamicColorSettingCodec.decode(preferences[DynamicColorKey]) }
        .distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DynamicColorKey] = enabled
        }
    }

    companion object {
        /**
         * Off by default. Funput's identity is its orange, and a wallpaper palette overrides it
         * with whatever the device happens to be wearing — lilac, green, anything. Users who want
         * the app to follow their wallpaper can turn it on.
         */
        const val DefaultEnabled = false

        private val DynamicColorKey = booleanPreferencesKey("app_dynamic_color")
    }
}

internal object DynamicColorSettingCodec {
    fun decode(value: Boolean?): Boolean = value ?: DynamicColorSettings.DefaultEnabled
}
