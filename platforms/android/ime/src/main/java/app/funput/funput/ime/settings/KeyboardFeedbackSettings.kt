package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.keyboard.sound.KeyboardSoundVolume
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
                hapticStrength = values[HapticStrengthKey],
                keyPreviewsEnabled = values[KeyPreviewsEnabledKey],
                soundStyle = values[SoundStyleKey],
                soundVolume = values[SoundVolumeKey],
            )
        }
        .distinctUntilChanged()

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[HapticsEnabledKey] = enabled }
    }

    /** Vibration off when [strength] is null, otherwise on at [strength], in one write. */
    suspend fun selectHaptics(strength: KeyboardHapticStrength?) {
        dataStore.edit { values ->
            values[HapticsEnabledKey] = strength != null
            if (strength != null) values[HapticStrengthKey] = strength.storageId
        }
    }

    suspend fun setKeyPreviewsEnabled(enabled: Boolean) {
        dataStore.edit { it[KeyPreviewsEnabledKey] = enabled }
    }

    /** Sound off when [style] is null, otherwise on in [style], in one write. */
    suspend fun selectSounds(style: KeyboardSoundStyle?) {
        dataStore.edit { values ->
            values[SoundsEnabledKey] = style != null
            if (style != null) values[SoundStyleKey] = style.storageId
        }
    }

    suspend fun setSoundVolume(volume: KeyboardSoundVolume) {
        dataStore.edit { it[SoundVolumeKey] = volume.storageId }
    }

    private companion object {
        val HapticsEnabledKey = booleanPreferencesKey("haptics_enabled")
        val SoundsEnabledKey = booleanPreferencesKey("sounds_enabled")
        val HapticStrengthKey = stringPreferencesKey("haptic_strength")
        val KeyPreviewsEnabledKey = booleanPreferencesKey("key_previews_enabled")
        val SoundStyleKey = stringPreferencesKey("sound_style")
        val SoundVolumeKey = stringPreferencesKey("sound_volume")
    }
}

internal object KeyboardFeedbackSettingCodec {
    fun decode(
        hapticsEnabled: Boolean?,
        soundsEnabled: Boolean?,
        hapticStrength: String? = null,
        keyPreviewsEnabled: Boolean? = null,
        soundStyle: String? = null,
        soundVolume: String? = null,
    ) = KeyboardFeedbackPreferences(
        hapticsEnabled = hapticsEnabled ?: KeyboardFeedbackPreferences.Default.hapticsEnabled,
        soundsEnabled = soundsEnabled ?: KeyboardFeedbackPreferences.Default.soundsEnabled,
        hapticStrength = KeyboardHapticStrength.fromStorage(hapticStrength),
        keyPreviewsEnabled = keyPreviewsEnabled ?: true,
        soundStyle = KeyboardSoundStyle.fromStorage(soundStyle),
        soundVolume = KeyboardSoundVolume.fromStorage(soundVolume),
    )
}
