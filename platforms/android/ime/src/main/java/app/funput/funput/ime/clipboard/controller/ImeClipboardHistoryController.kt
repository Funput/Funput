package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import java.util.UUID
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

internal class ImeClipboardHistoryController(
    parentScope: CoroutineScope,
    preferences: Flow<ClipboardPreferences>,
    private val storeFactory: (ClipboardExpiry) -> ClipboardHistoryStore,
    private val commitText: (String) -> Unit,
    private val afterCommit: () -> Unit,
    private val preparePanel: () -> Unit,
    private val onCleared: () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val mutableState = MutableStateFlow(ClipboardHistoryState())
    val state: StateFlow<ClipboardHistoryState> = mutableState.asStateFlow()
    private var prefs = ClipboardPreferences.Default
    private var store = storeFactory(prefs.expiry)
    private var mode = KeyboardEditorMode.TEXT
    private var active = false
    private var opened = false
    private var generation = 0L
    private var operation: Job? = null

    init { scope.launch { preferences.collect { updatePreferences(it) } } }

    fun start(editorMode: KeyboardEditorMode) {
        active = true
        mode = editorMode
        generation += 1
        publishAvailability()
    }

    fun stop() {
        active = false
        opened = false
        generation += 1
        operation?.cancel()
        mutableState.value = ClipboardHistoryState()
    }

    fun open() {
        if (!allowed()) return
        opened = true
        preparePanel()
        refresh()
    }

    fun viewChanged() {
        opened = false
        generation += 1
        operation?.cancel()
        publishAvailability()
    }

    fun paste(id: UUID, completed: (Boolean) -> Unit = {}) {
        if (!allowed() || operation?.isActive == true) return completed(false)
        val currentGeneration = generation
        val currentStore = store
        operation = scope.launch {
            val entry = withContext(ioDispatcher) { currentStore.load().firstOrNull { it.id == id } }
            if (entry == null || currentGeneration != generation || !allowed()) {
                operation = null
                refresh()
                return@launch completed(false)
            }
            commitText(entry.text)
            afterCommit()
            completed(true)
        }
    }

    fun togglePin(id: UUID, pinned: Boolean) = mutate { it.setPinned(pinned, id) }
    fun remove(id: UUID) = mutate { it.remove(id) }

    fun clear() {
        if (!allowed() || operation?.isActive == true) return
        val currentStore = store
        operation = scope.launch {
            withContext(ioDispatcher) { currentStore.clear() }
            mutableState.value = mutableState.value.copy(loading = false, entries = emptyList())
            onCleared()
        }
    }

    fun close() { stop(); scope.cancel() }

    private fun mutate(block: (ClipboardHistoryStore) -> List<ClipboardEntry>) {
        if (!allowed() || operation?.isActive == true) return
        val currentGeneration = generation
        val currentStore = store
        operation = scope.launch {
            val entries = withContext(ioDispatcher) { block(currentStore) }
            if (currentGeneration == generation && allowed()) {
                mutableState.value = mutableState.value.copy(loading = false, entries = entries)
            }
        }
    }

    private fun refresh() {
        if (!allowed() || operation?.isActive == true) return
        val currentGeneration = generation
        val currentStore = store
        mutableState.value = mutableState.value.copy(loading = true)
        operation = scope.launch {
            val entries = withContext(ioDispatcher) { currentStore.load() }
            if (currentGeneration == generation && allowed()) {
                mutableState.value = ClipboardHistoryState(true, false, entries)
            }
        }
    }

    private fun updatePreferences(value: ClipboardPreferences) {
        prefs = value
        store = storeFactory(value.expiry)
        generation += 1
        operation?.cancel()
        publishAvailability()
        if (opened && allowed()) refresh()
    }

    private fun publishAvailability() {
        mutableState.value = if (allowed()) mutableState.value.copy(available = true) else ClipboardHistoryState()
    }

    private fun allowed() = prefs.enabled && active && !mode.isPassword && !mode.usesKeypad
}
