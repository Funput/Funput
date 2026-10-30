package app.funput.funput.ime.settings.entersend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.ime.settings.funputSettingsStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Persists the apps whose Enter key sends the message. Empty by default for new and upgrading
 * installs alike: sending on Enter changes what a key has always done, so it is only ever chosen.
 */
class EnterToSendSettings internal constructor(
    private val dataStore: DataStore<Preferences>,
) : EnterToSendStoring {
    /** Uses the application's shared settings store; no additional file or migration is needed. */
    constructor(context: Context) : this(context.applicationContext.funputSettingsStore)

    override val packages: Flow<Set<String>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values -> EnterToSendSettingCodec.decode(values[PackagesKey]) }
        .distinctUntilChanged()

    override suspend fun add(packageName: String) {
        if (!EnterToSendSettingCodec.isPackageName(packageName)) return
        edit { it + packageName }
    }

    override suspend fun remove(packageName: String) = edit { it - packageName }

    private suspend fun edit(change: (Set<String>) -> Set<String>) {
        dataStore.edit { values ->
            val current = EnterToSendSettingCodec.decode(values[PackagesKey])
            values[PackagesKey] = EnterToSendSettingCodec.encode(change(current))
        }
    }

    internal companion object {
        val PackagesKey = stringPreferencesKey("enter_to_send_packages")
    }
}
