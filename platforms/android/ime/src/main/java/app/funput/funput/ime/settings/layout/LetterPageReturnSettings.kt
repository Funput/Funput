package app.funput.funput.ime.settings.layout

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

/**
 * Persists whether a space typed after punctuation on the symbol panel brings back the
 * letters. On by default for new and upgrading installs alike: an absent key decodes to it.
 */
class LetterPageReturnSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val enabled: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> LetterPageReturnSettingCodec.decode(preferences[EnabledKey]) }
        .distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[EnabledKey] = enabled }
    }

    companion object {
        const val DefaultEnabled = true
        private val EnabledKey = booleanPreferencesKey("returns_to_letters_after_punctuation")
    }
}

internal object LetterPageReturnSettingCodec {
    fun decode(value: Boolean?): Boolean = value ?: LetterPageReturnSettings.DefaultEnabled
}
