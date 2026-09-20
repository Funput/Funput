package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists keyboard placement independently from key sizing. */
class KeyboardPlacementSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val preferences: Flow<KeyboardPlacementPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { values -> KeyboardPlacementSettingCodec.decode(values[ModeKey], values[OffsetKey]) }
        .distinctUntilChanged()

    suspend fun setMode(mode: KeyboardPlacementMode) {
        dataStore.edit { it[ModeKey] = mode.storageId }
    }

    suspend fun setElevatedOffsetDp(offsetDp: Float) {
        dataStore.edit { it[OffsetKey] = KeyboardPlacementSettingCodec.validOffset(offsetDp) }
    }

    companion object {
        val DefaultPreferences = KeyboardPlacementPreferences.Default
        private val ModeKey = stringPreferencesKey("keyboard_placement_mode")
        private val OffsetKey = floatPreferencesKey("keyboard_elevated_offset_dp")
    }
}

internal object KeyboardPlacementSettingCodec {
    fun decode(mode: String?, offsetDp: Float?) = KeyboardPlacementPreferences(
        activeMode = KeyboardPlacementMode.fromStorageId(mode),
        elevatedOffsetDp = validOffset(offsetDp),
    )

    fun validOffset(value: Float?): Float = value
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: KeyboardPlacementPreferences.DefaultElevatedOffsetDp
}
