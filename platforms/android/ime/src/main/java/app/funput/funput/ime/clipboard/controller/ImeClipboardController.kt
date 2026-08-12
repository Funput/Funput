package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.policy.ClipboardOffer
import app.funput.funput.ime.clipboard.policy.ClipboardOfferPolicy
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ImeClipboardController(
    parentScope: CoroutineScope,
    preferences: Flow<ClipboardPreferences>,
    private val gateway: ClipboardGateway,
    private val storeFactory: (ClipboardExpiry) -> ClipboardHistoryStore,
    private val commitText: (String) -> Unit,
    private val afterCommit: () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val mutableOffer = MutableStateFlow<ClipboardOffer?>(null)
    val offer: StateFlow<ClipboardOffer?> = mutableOffer.asStateFlow()
    private var prefs = ClipboardPreferences.Default
    private var store = storeFactory(prefs.expiry)
    private var editorMode = KeyboardEditorMode.TEXT
    private var active = false
    private var generation = 0L
    private var suppressOffer = false
    private var observation: ClipboardObservation? = null
    private var refreshJob: Job? = null
    private var pasteJob: Job? = null
    init {
        scope.launch {
            preferences.collect { value ->
                prefs = value
                store = storeFactory(value.expiry)
                reconcileObservation()
                refreshOffer()
            }
        }
    }
    fun start(mode: KeyboardEditorMode) {
        active = true
        editorMode = mode
        suppressOffer = false
        generation += 1
        reconcileObservation()
        refreshOffer()
    }
    fun stop() {
        active = false
        generation += 1
        refreshJob?.cancel()
        pasteJob?.cancel()
        observation?.close()
        observation = null
        mutableOffer.value = null
    }
    fun pasteCurrent(onResult: (ClipboardPasteResult) -> Unit = {}) {
        if (pasteJob?.isActive == true) return onResult(ClipboardPasteResult.BUSY)
        val expected = mutableOffer.value
        val policy = policyContext()
        if (expected == null || !ClipboardOfferPolicy.allowsClipboard(policy)) {
            return onResult(ClipboardPasteResult.BLOCKED)
        }
        val pasteGeneration = generation
        mutableOffer.value = null
        pasteJob = scope.launch {
            val read = withContext(ioDispatcher) { gateway.readText(MaxTextLength) }
            if (!canCommit(pasteGeneration)) return@launch onResult(ClipboardPasteResult.CHANGED)
            if (read is ClipboardReadResult.Success) {
                if (expected.sourceToken != null && expected.sourceToken != read.sourceToken) {
                    refreshOffer()
                    return@launch onResult(ClipboardPasteResult.CHANGED)
                }
                commitText(read.text)
                afterCommit()
                if (!read.isSensitive && prefs.enabled) withContext(ioDispatcher) {
                    store.record(ClipboardEntry(
                        text = read.text,
                        sourceToken = read.sourceToken,
                    ))
                }
                suppressOffer = true
                onResult(ClipboardPasteResult.PASTED)
            } else {
                val result = read.toPasteResult()
                suppressOffer = result == ClipboardPasteResult.TOO_LARGE
                onResult(result)
            }
            refreshOffer()
        }
    }
    fun close() {
        stop()
        scope.cancel()
    }
    private fun reconcileObservation() {
        val shouldObserve = active && prefs.enabled
        if (shouldObserve && observation == null) {
            observation = gateway.observe {
                generation += 1
                suppressOffer = false
                refreshOffer()
            }
        } else if (!shouldObserve && observation != null) {
            observation?.close()
            observation = null
            mutableOffer.value = null
        }
    }

    private fun refreshOffer() {
        refreshJob?.cancel()
        if (suppressOffer || !ClipboardOfferPolicy.allowsClipboard(policyContext())) {
            mutableOffer.value = null
            return
        }
        val refreshGeneration = generation
        val snapshot = gateway.snapshot()
        refreshJob = scope.launch {
            val token = withContext(ioDispatcher) { store.lastCapturedSourceToken() }
            if (refreshGeneration == generation) {
                mutableOffer.value = ClipboardOfferPolicy.offer(snapshot, token, policyContext())
            }
        }
    }

    private fun canCommit(value: Long) = value == generation &&
        ClipboardOfferPolicy.allowsClipboard(policyContext())

    private fun policyContext() = ClipboardOfferPolicy.Context(prefs.enabled, active, editorMode)
    private companion object { const val MaxTextLength = 100_000 }
}
