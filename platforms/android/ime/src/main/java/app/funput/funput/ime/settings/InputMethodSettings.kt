package app.funput.funput.ime.settings

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.funput.funput.keyboard.model.KeyboardInputMethod
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.funputSettings by preferencesDataStore(name = "funput_settings")

/** Persists lightweight keyboard choices shared by the settings app and IME. */
class InputMethodSettings(context: Context) {
    private val dataStore = context.applicationContext.funputSettings

    val inputMethod: Flow<KeyboardInputMethod> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> InputMethodSettingCodec.decode(preferences[InputMethodKey]) }
        .distinctUntilChanged()

    suspend fun setInputMethod(method: KeyboardInputMethod) {
        dataStore.edit { preferences ->
            preferences[InputMethodKey] = InputMethodSettingCodec.encode(method)
        }
    }

    companion object {
        val DefaultInputMethod = KeyboardInputMethod.VNI

        private val InputMethodKey = stringPreferencesKey("input_method")
    }
}

internal object InputMethodSettingCodec {
    fun encode(method: KeyboardInputMethod): String = method.name

    fun decode(value: String?): KeyboardInputMethod =
        KeyboardInputMethod.entries.firstOrNull { it.name == value }
            ?: InputMethodSettings.DefaultInputMethod
}
