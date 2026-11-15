package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** User choices controlling local clipboard history. */
data class ClipboardPreferences(
    /** Whether Funput may add entries to history (explicit paste and optional system capture). */
    val enabled: Boolean,
    /** Retention applied to unpinned entries. */
    val expiry: ClipboardExpiry,
    /** When true with [enabled], record non-sensitive system copies while Funput is the IME. */
    val captureSystemCopies: Boolean = true,
) {
    /** Defaults shared by Settings and the IME. */
    companion object {
        /** History on with short retention; system-copy capture on while Funput is the IME. */
        val Default = ClipboardPreferences(
            enabled = true,
            expiry = ClipboardExpiry.Default,
            captureSystemCopies = true,
        )
    }
}

/** Persists clipboard preferences shared by the Settings app and IME. */
class ClipboardSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    /** Current preferences, falling back safely when the DataStore cannot be read. */
    val preferences: Flow<ClipboardPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { values ->
            ClipboardSettingCodec.decode(
                values[EnabledKey],
                values[ExpiryKey],
                values[CaptureSystemCopiesKey],
            )
        }
        .distinctUntilChanged()

    /** Enables or disables future clipboard history capture. */
    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[EnabledKey] = enabled }
    }

    /** Updates the retention window for unpinned entries. */
    suspend fun setExpiry(expiry: ClipboardExpiry) {
        dataStore.edit { it[ExpiryKey] = ClipboardSettingCodec.encode(expiry) }
    }

    /** Enables or disables recording every non-sensitive system copy while Funput is the IME. */
    suspend fun setCaptureSystemCopies(enabled: Boolean) {
        dataStore.edit { it[CaptureSystemCopiesKey] = enabled }
    }

    private companion object {
        val EnabledKey = booleanPreferencesKey("clipboard_history_enabled")
        val ExpiryKey = stringPreferencesKey("clipboard_history_expiry")
        val CaptureSystemCopiesKey = booleanPreferencesKey("clipboard_capture_system_copies")
    }
}

internal object ClipboardSettingCodec {
    fun decode(enabled: Boolean?, expiry: String?, captureSystemCopies: Boolean? = null) =
        ClipboardPreferences(
            enabled = enabled ?: ClipboardPreferences.Default.enabled,
            expiry = ClipboardExpiry.fromStorageValue(expiry),
            captureSystemCopies = captureSystemCopies
                ?: ClipboardPreferences.Default.captureSystemCopies,
        )

    fun encode(expiry: ClipboardExpiry): String = expiry.storageValue
}
