package app.funput.funput.ui.shortcuts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.shortcuts.persistence.ShortcutsStoring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ShortcutsScreenModel(
    private val store: ShortcutsStoring,
    private val scope: CoroutineScope,
) {
    var library by mutableStateOf(ShortcutLibrary())
        private set
    var hasLoaded by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<ShortcutsStorageError?>(null)
        private set
    var saveError by mutableStateOf<ShortcutsStorageError?>(null)
        private set
    var query by mutableStateOf("")

    val canWrite get() = hasLoaded && loadError == null && !isLoading && !isSaving
    val filteredEntries: List<TextShortcut>
        get() {
            val search = query.trim()
            return if (search.isEmpty()) library.entries else library.entries.filter {
                it.trigger.contains(search, ignoreCase = true) ||
                    it.expansion.contains(search, ignoreCase = true)
            }
        }

    fun reload() {
        if (isLoading || isSaving) return
        isLoading = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { store.load() } }
                .onSuccess { value -> library = value; hasLoaded = true; loadError = null }
                .onFailure { loadError = it.storageError() }
            isLoading = false
        }
    }

    fun isDuplicate(entry: TextShortcut) = library.isDuplicate(entry)
    fun contains(entry: TextShortcut) = library.entries.any { it.id == entry.id }

    fun save(entry: TextShortcut, saved: () -> Unit) {
        if (!canWrite || !entry.isValid) return
        if (isDuplicate(entry)) return Unit.also { saveError = ShortcutsStorageError.DuplicateTrigger }
        val isNew = !contains(entry)
        val entries = library.entries.toMutableList()
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index < 0) entries += entry else entries[index] = entry
        commit(library.copy(entries = entries), success = {
            if (isNew) query = ""
            saved()
        })
    }

    fun delete(entry: TextShortcut, deleted: () -> Unit = {}) {
        if (!canWrite) return
        commit(library.copy(entries = library.entries.filterNot { it.id == entry.id }), deleted)
    }

    fun updateOptions(edit: (ShortcutLibrary) -> ShortcutLibrary) {
        if (!canWrite) return
        val previous = library
        val candidate = edit(previous)
        library = candidate
        commit(candidate, failed = { library = previous })
    }

    fun clearSaveError() { saveError = null }

    private fun commit(
        candidate: ShortcutLibrary,
        success: () -> Unit = {},
        failed: () -> Unit = {},
    ) {
        isSaving = true
        saveError = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { store.save(candidate) } }
                .onSuccess { library = candidate; success() }
                .onFailure { error ->
                    saveError = error.storageError()
                    if (saveError !in listOf(ShortcutsStorageError.WriteFailed,
                            ShortcutsStorageError.DuplicateTrigger)) loadError = saveError
                    failed()
                }
            isSaving = false
        }
    }
}

private fun Throwable.storageError() = this as? ShortcutsStorageError
    ?: ShortcutsStorageError.WriteFailed
