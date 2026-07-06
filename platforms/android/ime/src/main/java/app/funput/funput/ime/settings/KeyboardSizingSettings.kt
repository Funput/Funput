package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists keyboard geometry presets shared by the settings app and IME. */
class KeyboardSizingSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val profile: Flow<KeyboardSizingProfile> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> KeyboardSizingSettingCodec.decode(preferences[ProfileKey]) }
        .distinctUntilChanged()

    suspend fun setProfile(profile: KeyboardSizingProfile) {
        dataStore.edit { preferences ->
            preferences[ProfileKey] = KeyboardSizingSettingCodec.encode(profile)
        }
    }

    companion object {
        val DefaultProfile = KeyboardSizingProfile.Default

        private val ProfileKey = stringPreferencesKey("keyboard_sizing_profile")
    }
}

internal object KeyboardSizingSettingCodec {
    fun encode(profile: KeyboardSizingProfile): String = profile.id

    fun decode(value: String?): KeyboardSizingProfile = KeyboardSizingProfile.fromId(value)
}
