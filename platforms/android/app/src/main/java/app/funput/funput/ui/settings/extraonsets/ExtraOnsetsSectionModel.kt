package app.funput.funput.ui.settings.extraonsets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetter
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetsStoring
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Writes atomically while the UI continues to display the store's committed selection. */
internal class ExtraOnsetsSectionModel(
    private val store: ExtraOnsetsStoring,
    private val scope: CoroutineScope,
) {
    var isSaving by mutableStateOf(false)
        private set
    var hasSaveError by mutableStateOf(false)
        private set

    fun setEnabled(enabled: Boolean) = save { store.setEnabled(enabled) }

    fun setLetter(letter: ExtraOnsetLetter, enabled: Boolean) = save { store.setLetter(letter, enabled) }

    private fun save(write: suspend () -> Unit) {
        if (isSaving) return
        hasSaveError = false
        // Guard before launching so two taps cannot schedule competing writes.
        isSaving = true
        scope.launch {
            try {
                write()
            } catch (_: IOException) {
                hasSaveError = true
            } finally {
                isSaving = false
            }
        }
    }
}
