package app.funput.funput.ime

import android.content.Context
import app.funput.funput.ime.settings.KeyboardPlacementSettings
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import kotlinx.coroutines.CoroutineScope

/** Isolates placement observation from engine-affecting IME settings. */
internal class ImePlacementSettingsState(private val onChanged: () -> Unit) {
    var preferences = KeyboardPlacementSettings.DefaultPreferences
        private set

    fun observe(context: Context, scope: CoroutineScope) {
        KeyboardPlacementSettings(context).preferences.collectIn(scope) { value ->
            if (value == preferences) return@collectIn
            preferences = value
            onChanged()
        }
    }
}
