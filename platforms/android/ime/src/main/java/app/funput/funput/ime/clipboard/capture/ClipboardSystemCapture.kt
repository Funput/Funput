package app.funput.funput.ime.clipboard.capture

import app.funput.funput.ime.clipboard.controller.ClipboardObservationSlot
import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * While Funput is the selected IME, optionally records non-sensitive system clipboard text.
 * Pauses in password/keypad editors. Reads/writes stay on [ioDispatcher], off the typing path.
 */
internal class ClipboardSystemCapture(
    parentScope: CoroutineScope,
    preferences: Flow<ClipboardPreferences>,
    private val gateway: ClipboardGateway,
    private val storeFactory: (ClipboardExpiry) -> ClipboardHistoryStore,
    private val onRecorded: () -> Unit = {},
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxTextLength: Int = MaxTextLength,
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private var prefs = ClipboardPreferences.Default
    private var store = storeFactory(prefs.expiry)
    private var inputViewActive = false
    private var editorMode = KeyboardEditorMode.TEXT
    private val observation = ClipboardObservationSlot()
    private var captureJob: Job? = null

    init {
        scope.launch {
            preferences.collect { value ->
                prefs = value
                store = storeFactory(value.expiry)
                reconcile()
            }
        }
    }

    fun start(mode: KeyboardEditorMode) {
        inputViewActive = true
        editorMode = mode
        reconcile()
    }

    fun stop() {
        inputViewActive = false
        captureJob?.cancel()
        reconcile()
    }

    fun close() {
        captureJob?.cancel()
        observation.close()
        scope.cancel()
    }

    private fun reconcile() {
        if (!shouldCapture()) {
            captureJob?.cancel()
            observation.close()
            return
        }
        observation.open(scope, gateway::observe, ::shouldCapture, ::captureCurrent)
    }

    private fun shouldCapture(): Boolean {
        if (!(prefs.enabled && prefs.captureSystemCopies)) return false
        if (inputViewActive && (editorMode.isPassword || editorMode.usesKeypad)) return false
        return true
    }

    private fun captureCurrent() {
        if (!shouldCapture()) return
        val captureStore = store
        captureJob?.cancel()
        captureJob = scope.launch {
            val read = withContext(ioDispatcher) { gateway.readText(maxTextLength) }
            if (!shouldCapture() || captureStore !== store) return@launch
            if (read !is ClipboardReadResult.Success || read.isSensitive) return@launch
            withContext(ioDispatcher) {
                if (!shouldCapture() || captureStore !== store) return@withContext
                captureStore.record(ClipboardEntry(text = read.text, sourceToken = read.sourceToken))
            }
            if (shouldCapture() && captureStore === store) onRecorded()
        }
    }

    private companion object {
        const val MaxTextLength = 100_000
    }
}
