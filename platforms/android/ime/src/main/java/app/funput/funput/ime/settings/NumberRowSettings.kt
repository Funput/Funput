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

/** Persists whether Telex-family layouts show the top digit row. */
class NumberRowSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val showsNumberRow: Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> NumberRowSettingCodec.decode(preferences[ShowsNumberRowKey]) }
        .distinctUntilChanged()

    suspend fun setShowsNumberRow(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ShowsNumberRowKey] = enabled
        }
    }

    companion object {
        const val DefaultShowsNumberRow = false

        private val ShowsNumberRowKey = booleanPreferencesKey("shows_number_row")
    }
}

internal object NumberRowSettingCodec {
    fun decode(value: Boolean?): Boolean = value ?: NumberRowSettings.DefaultShowsNumberRow
}
