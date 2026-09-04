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
            // The cohort is decided once, by the migration. Reading it back rather than
            // re-deriving it keeps that decision in one place; when it is missing the
            // store did not answer, which is a reason to hold still, not to upgrade.
            val default = ToneStyleDefaultCohort.of(preferences[ToneStyleDefaultCohortKey])
                ?.default ?: FallbackToneStyle
            ToneStyleSettingCodec.decode(preferences[ToneStyleKey], default)
        }
        .distinctUntilChanged()

    suspend fun setToneStyle(style: ToneStyle) {
        dataStore.edit { preferences ->
            preferences[ToneStyleKey] = ToneStyleSettingCodec.encode(style)
        }
    }

    companion object {
        /**
         * Placement to assume while storage has not answered yet — before the first flow
         * emission, or after a read fails. Traditional because guessing wrong here moves
         * the tone under someone mid-word, and only one of the two guesses can do that to
         * a user who never asked for a change.
         */
        val FallbackToneStyle = ToneStyle.TRADITIONAL

        internal val ToneStyleKey = stringPreferencesKey("tone_style")
    }
}
