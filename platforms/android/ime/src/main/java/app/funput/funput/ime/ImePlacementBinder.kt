package app.funput.funput.ime

import android.content.Context
import app.funput.funput.ime.settings.KeyboardPlacementSettings
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Persists placement changes emitted by the live IME controls. */
internal object ImePlacementBinder {
    fun bind(view: FunputKeyboardView, context: Context, scope: CoroutineScope) {
        val store = KeyboardPlacementSettings(context)
        view.callbacks.onPlacementChanged = { preferences ->
            scope.launch { store.setPreferences(preferences) }
        }
    }
}
