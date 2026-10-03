package app.funput.funput.ime.settings.speech

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

/** User preference; independent of the build's speech feature gate. */
class VoiceInputSettings(context: Context) {
    private val store = context.applicationContext.funputSettingsStore

    val voiceInputEnabled: Flow<Boolean> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { VoiceInputSettingCodec.decode(it[EnabledKey]) }
        .distinctUntilChanged()

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        store.edit { it[EnabledKey] = enabled }
    }

    companion object {
        const val DefaultVoiceInputEnabled = true
        private val EnabledKey = booleanPreferencesKey("voice_input_enabled")
    }
}

internal object VoiceInputSettingCodec {
    fun decode(value: Boolean?): Boolean = value ?: VoiceInputSettings.DefaultVoiceInputEnabled
}
