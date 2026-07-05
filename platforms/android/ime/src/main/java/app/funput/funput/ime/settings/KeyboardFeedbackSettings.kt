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

/** Persists physical feedback preferences shared by Settings and the IME. */
class KeyboardFeedbackSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val preferences: Flow<KeyboardFeedbackPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            KeyboardFeedbackSettingCodec.decode(
                hapticsEnabled = values[HapticsEnabledKey],
                soundsEnabled = values[SoundsEnabledKey],
            )
        }
        .distinctUntilChanged()

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HapticsEnabledKey] = enabled }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        dataStore.edit { it[SoundsEnabledKey] = enabled }
    }

    private companion object {
        val HapticsEnabledKey = booleanPreferencesKey("haptics_enabled")
        val SoundsEnabledKey = booleanPreferencesKey("sounds_enabled")
    }
}

internal object KeyboardFeedbackSettingCodec {
    fun decode(
        hapticsEnabled: Boolean?,
        soundsEnabled: Boolean?,
    ) = KeyboardFeedbackPreferences(
        hapticsEnabled = hapticsEnabled ?: KeyboardFeedbackPreferences.Default.hapticsEnabled,
        soundsEnabled = soundsEnabled ?: KeyboardFeedbackPreferences.Default.soundsEnabled,
    )
}
