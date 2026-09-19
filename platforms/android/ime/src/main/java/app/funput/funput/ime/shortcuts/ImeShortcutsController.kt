package app.funput.funput.ime.shortcuts

import android.util.Log
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.shortcuts.persistence.ShortcutsStoring
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One cancellable, read-only shortcut load per input activation. */
internal class ImeShortcutsController(
    private val scope: CoroutineScope,
    private val store: ShortcutsStoring,
    private val begin: () -> Unit,
    private val receive: (ShortcutLibrary) -> Unit,
) {
    private var generation = 0L
    private var job: Job? = null

    fun activate() {
        cancel()
        begin()
        val expected = generation
        job = scope.launch {
            val value = try {
                withContext(Dispatchers.IO) { store.load() }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (generation == expected) logFailure(error)
                return@launch
            }
            if (generation == expected) receive(value)
        }
    }

    fun cancel() {
        generation += 1
        job?.cancel()
        job = null
    }

    private fun logFailure(error: Throwable) {
        val kind = when (error) {
            ShortcutsStorageError.Unavailable -> "unavailable"
            ShortcutsStorageError.ReadFailed -> "read"
            ShortcutsStorageError.InvalidData -> "invalid-data"
            ShortcutsStorageError.UnsupportedVersion -> "unsupported-version"
            else -> "unexpected"
        }
        Log.e(LogTag, "Shortcut load failed: $kind")
    }

    private companion object {
        const val LogTag = "FunputShortcuts"
    }
}
