package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class PersonalSuggestionPreferences(
    val enabled: Boolean,
    val resetToken: String?,
    val appliedResetToken: String?,
) {
    companion object {
        val Default = PersonalSuggestionPreferences(true, null, null)
    }
}

internal fun PersonalSuggestionPreferences.pendingReset(inFlight: String?): String? =
    resetToken?.takeIf { it != appliedResetToken && it != inFlight }

class PersonalSuggestionSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val preferences: Flow<PersonalSuggestionPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { values ->
            PersonalSuggestionPreferences(
                enabled = values[EnabledKey] ?: true,
                resetToken = values[ResetTokenKey],
                appliedResetToken = values[AppliedResetTokenKey],
            )
        }
        .distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[EnabledKey] = enabled }
    }

    suspend fun requestReset() {
        dataStore.edit { it[ResetTokenKey] = UUID.randomUUID().toString() }
    }

    suspend fun acknowledgeReset(token: String) {
        dataStore.edit { values ->
            if (values[ResetTokenKey] == token) values[AppliedResetTokenKey] = token
        }
    }

    private companion object {
        val EnabledKey = booleanPreferencesKey("personal_suggestions_enabled")
        val ResetTokenKey = stringPreferencesKey("personal_suggestions_reset_token")
        val AppliedResetTokenKey = stringPreferencesKey("personal_suggestions_applied_reset_token")
    }
}
