package app.funput.funput.ime.settings.gestures

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

/** Persists the single switch that gates every smart gesture. */
class SmartGestureSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val enabled: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> SmartGestureSettingCodec.decode(preferences[EnabledKey]) }
        .distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[EnabledKey] = enabled }
    }

    companion object {
        const val DefaultEnabled = true
        private val EnabledKey = booleanPreferencesKey("smart_gestures_enabled")
    }
}

internal object SmartGestureSettingCodec {
    fun decode(value: Boolean?): Boolean = value ?: SmartGestureSettings.DefaultEnabled
}
