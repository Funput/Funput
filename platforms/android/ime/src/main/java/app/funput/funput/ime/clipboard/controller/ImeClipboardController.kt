package app.funput.funput.ime.clipboard.controller
import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.policy.ClipboardOffer
import app.funput.funput.ime.clipboard.policy.ClipboardOfferPolicy
import app.funput.funput.ime.clipboard.policy.matches
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
    private val session = ClipboardSessionGuard()
    private var suppressOffer = false
    private val observation = ClipboardObservationSlot()
    private var refreshJob: Job? = null
    private var pasteJob: Job? = null
    init {
        scope.launch {
            preferences.collect { value ->
                if (value == prefs) return@collect
                prefs = value
                store = storeFactory(value.expiry)
                session.invalidate()
                pasteJob?.cancel()
                reconcileObservation()
                refreshOffer()
            }
        }
    }
    fun start(mode: KeyboardEditorMode) {
        session.start(mode)
        suppressOffer = false
        reconcileObservation()
        refreshOffer()
    }
    fun stop() {
        session.stop()
        refreshJob?.cancel()
        pasteJob?.cancel()
        observation.close()
        mutableOffer.value = null
    }
    fun pasteCurrent(onResult: (ClipboardPasteResult) -> Unit = {}) {
        if (pasteJob?.isActive == true) return onResult(ClipboardPasteResult.BUSY)
        val expected = mutableOffer.value
        val policy = policyContext()
        if (expected == null || !ClipboardOfferPolicy.allowsClipboard(policy)) {
            return onResult(ClipboardPasteResult.BLOCKED)
        }
        val pasteGeneration = session.token
        val pasteStore = store
        mutableOffer.value = null
        pasteJob = scope.launch {
            val read = withContext(ioDispatcher) { gateway.readText(MaxTextLength) }
            if (!canCommit(pasteGeneration, pasteStore)) {
                return@launch onResult(ClipboardPasteResult.CHANGED)
            }
            if (read is ClipboardReadResult.Success) {
                if (!expected.matches(read)) {
                    refreshOffer()
                    return@launch onResult(ClipboardPasteResult.CHANGED)
                }
                commitText(read.text)
                afterCommit()
                if (!read.isSensitive && canCommit(pasteGeneration, pasteStore)) {
                    withContext(ioDispatcher) { pasteStore.record(ClipboardEntry(
                        text = read.text,
                        sourceToken = read.sourceToken,
                    )) }
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
    fun close() { stop(); scope.cancel() }
    fun historyCleared() { suppressOffer = false; refreshOffer() }
    private fun reconcileObservation() {
        val shouldObserve = session.isActive && prefs.enabled
        if (shouldObserve) {
            observation.open(scope, gateway::observe, {
                session.isActive && prefs.enabled
            }) {
                session.invalidate()
                suppressOffer = false
                refreshOffer()
            }
        } else {
            observation.close()
            mutableOffer.value = null
        }
    }
    private fun refreshOffer() {
        refreshJob?.cancel()
        if (suppressOffer || !ClipboardOfferPolicy.allowsClipboard(policyContext())) {
            mutableOffer.value = null
            return
        }
        val refreshGeneration = session.token
        val refreshStore = store
        val snapshot = gateway.snapshot()
        refreshJob = scope.launch {
            val token = withContext(ioDispatcher) { refreshStore.lastCapturedSourceToken() }
            if (session.matches(refreshGeneration) && store === refreshStore) {
                mutableOffer.value = ClipboardOfferPolicy.offer(snapshot, token, policyContext())
            }
        }
    }
    private fun canCommit(value: Long, expectedStore: ClipboardHistoryStore) =
        session.matches(value) && store === expectedStore &&
            ClipboardOfferPolicy.allowsClipboard(policyContext())
    private fun policyContext() = ClipboardOfferPolicy.Context(
        prefs.enabled, session.isActive, session.editorMode,
    )
    private companion object { const val MaxTextLength = 100_000 }
}
