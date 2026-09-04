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

/** Persists tone-mark placement style shared by the settings app and IME. */
class ToneStyleSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val toneStyle: Flow<ToneStyle> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            val cohort = ToneStyleDefaultCohort.resolve(
                value = preferences[ToneStyleDefaultCohortKey],
                hasPriorSettings = preferences.asMap().isNotEmpty(),
                isUpgrade = false,
            )
            ToneStyleSettingCodec.decode(preferences[ToneStyleKey], cohort.default)
        }
        .distinctUntilChanged()

    suspend fun setToneStyle(style: ToneStyle) {
        dataStore.edit { preferences ->
            preferences[ToneStyleKey] = ToneStyleSettingCodec.encode(style)
        }
    }

    companion object {
        val DefaultToneStyle = ToneStyle.MODERN

        internal val ToneStyleKey = stringPreferencesKey("tone_style")
    }
}
