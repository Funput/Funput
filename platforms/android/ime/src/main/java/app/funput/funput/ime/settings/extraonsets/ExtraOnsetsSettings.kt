package app.funput.funput.ime.settings.extraonsets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.ime.settings.funputSettingsStore

/** Persists extra onsets in the same DataStore snapshot as the other composition options. */
class ExtraOnsetsSettings internal constructor(private val dataStore: DataStore<Preferences>) : ExtraOnsetsStoring {
    /** Uses the application's shared settings store; no additional file or migration is needed. */
    constructor(context: Context) : this(context.applicationContext.funputSettingsStore)

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { values ->
            values[SelectionKey] = (if (enabled) ExtraOnsetLetters.All else ExtraOnsetLetters.None).configurationValue
        }
    }

    override suspend fun setLetter(letter: ExtraOnsetLetter, enabled: Boolean) {
        dataStore.edit { values ->
            val current = ExtraOnsetLetters.parse(values[SelectionKey])
            values[SelectionKey] = current.withLetter(letter, enabled).configurationValue
        }
    }

    internal companion object {
        val SelectionKey = stringPreferencesKey("extra_onsets")
    }
}
