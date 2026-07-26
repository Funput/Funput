package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.funput.funput.theme.KeyboardThemeId
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists keyboard theme presets shared by the settings app and IME. */
class KeyboardThemeSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettingsStore

    val selection: Flow<KeyboardThemeSelection> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(KeyboardThemeSettingCodec::decode)
        .distinctUntilChanged()

    suspend fun setTheme(themeId: KeyboardThemeId, slot: KeyboardThemeSlot) {
        dataStore.edit { preferences -> preferences[slot.key] = themeId.value }
    }

    suspend fun setFollowsAppearance(follows: Boolean) {
        dataStore.edit { preferences -> preferences[FollowsAppearanceKey] = follows }
    }

    companion object {
        val DefaultThemeId = KeyboardThemeId.Default
        val DefaultSelection = KeyboardThemeSelection()

        internal val ThemeIdKey = stringPreferencesKey("keyboard_theme_id")
        internal val LightThemeIdKey = stringPreferencesKey("keyboard_theme_id_light")
        internal val DarkThemeIdKey = stringPreferencesKey("keyboard_theme_id_dark")
        internal val FollowsAppearanceKey =
            booleanPreferencesKey("keyboard_theme_follows_appearance")

        private val KeyboardThemeSlot.key
            get() = when (this) {
                KeyboardThemeSlot.SINGLE -> ThemeIdKey
                KeyboardThemeSlot.LIGHT -> LightThemeIdKey
                KeyboardThemeSlot.DARK -> DarkThemeIdKey
            }
    }
}

internal object KeyboardThemeSettingCodec {
    fun encode(themeId: KeyboardThemeId): String = themeId.value

    fun decode(value: String?): KeyboardThemeId =
        KeyboardThemeId.parseOrNull(value) ?: KeyboardThemeId.Default

    /**
     * Reads the whole selection.
     *
     * The per-appearance slots fall back to the single theme rather than to a preset, so someone
     * upgrading from a build that stored one id keeps that theme in every slot and sees nothing
     * change until they turn following on themselves.
     */
    fun decode(preferences: Preferences): KeyboardThemeSelection {
        val single = decode(preferences[KeyboardThemeSettings.ThemeIdKey])
        return KeyboardThemeSelection(
            singleThemeId = single,
            lightThemeId = preferences[KeyboardThemeSettings.LightThemeIdKey]
                ?.let(KeyboardThemeId::parseOrNull) ?: single,
            darkThemeId = preferences[KeyboardThemeSettings.DarkThemeIdKey]
                ?.let(KeyboardThemeId::parseOrNull) ?: single,
            followsAppearance = preferences[KeyboardThemeSettings.FollowsAppearanceKey] ?: false,
        )
    }
}
