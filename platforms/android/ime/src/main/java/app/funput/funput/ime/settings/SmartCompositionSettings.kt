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

/** Persists smart composition preferences shared by the settings app and IME. */
class SmartCompositionSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val preferences: Flow<SmartCompositionPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            SmartCompositionSettingCodec.decode(
                spellCheckEnabled = values[SpellCheckEnabledKey],
                smartRestoreEnabled = values[SmartRestoreEnabledKey],
                autoCapitalizeEnabled = values[AutoCapitalizeEnabledKey],
            )
        }
        .distinctUntilChanged()

    suspend fun setSpellCheckEnabled(enabled: Boolean) {
        dataStore.edit { it[SpellCheckEnabledKey] = enabled }
    }

    suspend fun setSmartRestoreEnabled(enabled: Boolean) {
        dataStore.edit { it[SmartRestoreEnabledKey] = enabled }
    }

    suspend fun setAutoCapitalizeEnabled(enabled: Boolean) {
        dataStore.edit { it[AutoCapitalizeEnabledKey] = enabled }
    }

    private companion object {
        val SpellCheckEnabledKey = booleanPreferencesKey("spell_check_enabled")
        val SmartRestoreEnabledKey = booleanPreferencesKey("smart_restore_enabled")
        val AutoCapitalizeEnabledKey = booleanPreferencesKey("auto_capitalize_enabled")
    }
}
