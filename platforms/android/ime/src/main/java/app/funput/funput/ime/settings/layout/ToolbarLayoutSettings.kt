package app.funput.funput.ime.settings.layout

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.ime.settings.funputSettingsStore
import app.funput.funput.keyboard.layout.toolbar.ToolbarArrangement
import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists the ordered leading/trailing toolbar sides shared by Settings and the IME. */
data class ToolbarLayoutPreferences(
    val leading: List<ToolbarSlot>,
    val trailing: List<ToolbarSlot>,
) {
    fun toArrangement(): ToolbarArrangement = ToolbarArrangement(leading, trailing)

    companion object {
        val Default = ToolbarLayoutPreferences(
            leading = ToolbarArrangement.Default.leading,
            trailing = ToolbarArrangement.Default.trailing,
        )
    }
}

/** Reads and writes [ToolbarLayoutPreferences] in the shared Funput settings DataStore. */
class ToolbarLayoutSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val preferences: Flow<ToolbarLayoutPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { values -> ToolbarLayoutSettingCodec.decode(values[LeadingKey], values[TrailingKey]) }
        .distinctUntilChanged()

    suspend fun setPreferences(value: ToolbarLayoutPreferences) {
        dataStore.edit { write(it, value) }
    }

    /** Read-modify-write inside one DataStore transaction so rapid edits do not clobber each other. */
    suspend fun updatePreferences(transform: (ToolbarLayoutPreferences) -> ToolbarLayoutPreferences) {
        dataStore.edit { values ->
            val current = ToolbarLayoutSettingCodec.decode(values[LeadingKey], values[TrailingKey])
            write(values, transform(current))
        }
    }

    private fun write(values: MutablePreferences, value: ToolbarLayoutPreferences) {
        values[LeadingKey] = ToolbarLayoutSettingCodec.encode(value.leading)
        values[TrailingKey] = ToolbarLayoutSettingCodec.encode(value.trailing)
    }

    private companion object {
        // v2 stores both sides left-to-right; older RTL trailing keys are ignored.
        val LeadingKey = stringPreferencesKey("toolbar_layout_leading_v2")
        val TrailingKey = stringPreferencesKey("toolbar_layout_trailing_v2")
    }
}

internal object ToolbarLayoutSettingCodec {
    fun decode(leading: String?, trailing: String?): ToolbarLayoutPreferences {
        val parsedLeading = parse(leading) ?: return ToolbarLayoutPreferences.Default
        val parsedTrailing = parse(trailing) ?: return ToolbarLayoutPreferences.Default
        val arrangement = ToolbarArrangement(parsedLeading, parsedTrailing)
            .normalize(ToolbarSlot.entries.toSet())
        return ToolbarLayoutPreferences(arrangement.leading, arrangement.trailing)
    }

    fun encode(slots: List<ToolbarSlot>): String = slots.joinToString(",") { it.name }

    private fun parse(raw: String?): List<ToolbarSlot>? {
        if (raw == null) return null
        if (raw.isEmpty()) return emptyList()
        return raw.split(',').mapNotNull { name ->
            if (name == LegacySystemSlot) return@mapNotNull null
            ToolbarSlot.entries.firstOrNull { it.name == name } ?: return null
        }
    }

    private const val LegacySystemSlot = "SYSTEM_INPUT_METHOD"
}
