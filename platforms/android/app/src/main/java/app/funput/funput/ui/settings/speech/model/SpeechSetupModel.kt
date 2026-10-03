package app.funput.funput.ui.settings.speech.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.ime.speech.model.SpeechLocale
import app.funput.funput.ime.speech.preparation.SpeechAvailability
import app.funput.funput.ime.speech.preparation.SpeechPreparationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class SpeechSetupModel(
    private val service: SpeechPreparationService,
    enabled: Flow<Boolean>,
    private val saveEnabled: suspend (Boolean) -> Unit,
    owner: CoroutineScope,
    featureAvailable: Boolean,
) {
    private val scope = CoroutineScope(owner.coroutineContext + SupervisorJob(owner.coroutineContext[Job]))
    var state by mutableStateOf(SpeechSetupState(featureAvailable = featureAvailable))
        private set
    private var closed = false
    private var resumed = false
    private val operations = SpeechLocaleOperations(service) { locale, value ->
        state = state.copy(locales = state.locales + (locale to value))
    }

    init {
        scope.launch { enabled.collect { state = state.copy(enabled = it) } }
    }

    fun resume(permissionGranted: Boolean) {
        if (closed) return
        state = state.copy(permissionGranted = permissionGranted)
        if (resumed) return
        resumed = true
        if (!state.featureAvailable) return
        state = state.copy(availability = service.availability())
        operations.stop()
        if (state.availability == SpeechAvailability.AVAILABLE) operations.resume()
    }

    fun pause() {
        resumed = false
        operations.stop()
    }
    fun permissionResult(granted: Boolean) {
        state = state.copy(permissionGranted = granted)
    }

    fun refresh(locale: SpeechLocale) = operations.check(locale)
    fun download(locale: SpeechLocale) = operations.download(locale)

    fun setEnabled(value: Boolean) {
        if (closed || !state.featureAvailable || state.saving || value == state.enabled) return
        state = state.copy(saving = true, saveError = false)
        scope.launch {
            try {
                saveEnabled(value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(saveError = true)
            } finally {
                state = state.copy(saving = false)
            }
        }
    }

    fun close() {
        closed = true
        operations.stop()
        scope.cancel()
    }
}
