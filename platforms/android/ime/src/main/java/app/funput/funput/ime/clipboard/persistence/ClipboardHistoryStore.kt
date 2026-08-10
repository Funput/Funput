package app.funput.funput.ime.clipboard.persistence

import android.content.Context
import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Blocking, file-backed clipboard history. Call its methods away from the main thread.
 *
 * Corrupt or unavailable storage degrades to an empty history instead of crashing the IME.
 *
 * @param directory Private directory that will contain `clipboard.json`.
 * @param expiry Retention applied to unpinned entries.
 */
class ClipboardHistoryStore(
    directory: File,
    private val expiry: ClipboardExpiry = ClipboardExpiry.Default,
) {
    private val historyFile = File(directory, FileName)
    private val file = AtomicJsonFile(historyFile)
    private val lock = ClipboardStoreLocks.forFile(historyFile)

    /** Returns live entries in newest-first order without rewriting expired data. */
    fun load(now: Instant = Instant.now()): List<ClipboardEntry> = synchronized(lock) {
        prune(read().items, now)
    }

    /** Returns the last clipboard generation explicitly captured by Funput. */
    fun lastCapturedSourceToken(): String? = synchronized(lock) {
        read().lastCapturedSourceToken
    }

    /** Records an entry, moves duplicate text to the front, and applies retention limits. */
    fun record(entry: ClipboardEntry, now: Instant = Instant.now()): List<ClipboardEntry> =
        synchronized(lock) {
            val current = read()
            val items = prune(listOf(entry) + current.items.filter { it.text != entry.text }, now)
            write(ClipboardHistoryPayload(entry.sourceToken, items))
            items
        }

    /** Pins or unpins an entry and returns the resulting live history. */
    fun setPinned(
        isPinned: Boolean,
        id: UUID,
        now: Instant = Instant.now(),
    ): List<ClipboardEntry> = synchronized(lock) {
        val current = read()
        val index = current.items.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized prune(current.items, now)
        val changed = current.items.toMutableList().apply {
            this[index] = this[index].copy(isPinned = isPinned)
        }
        val items = prune(changed, now)
        write(current.copy(items = items))
        items
    }

    /** Removes one entry while preserving the last captured source token. */
    fun remove(id: UUID, now: Instant = Instant.now()): List<ClipboardEntry> =
        synchronized(lock) {
            val current = read()
            val items = prune(current.items.filter { it.id != id }, now)
            write(current.copy(items = items))
            items
        }

    /** Clears every entry, including pinned items, and resets the captured source token. */
    fun clear() = synchronized(lock) {
        write(ClipboardHistoryPayload.Empty)
    }

    private fun read(): ClipboardHistoryPayload = file.read()
        ?.let { runCatching { ClipboardHistoryJsonCodec.decode(it) }.getOrNull() }
        ?: ClipboardHistoryPayload.Empty

    private fun write(payload: ClipboardHistoryPayload) {
        file.write(ClipboardHistoryJsonCodec.encode(payload))
    }

    private fun prune(items: List<ClipboardEntry>, now: Instant): List<ClipboardEntry> {
        val live = items.filter { it.isPinned || it.capturedAt.plus(expiry.duration).isAfter(now) }
        var excess = live.size - Limit
        if (excess <= 0) return live
        val kept = ArrayList<ClipboardEntry>(live.size)
        live.asReversed().forEach { entry ->
            if (excess > 0 && !entry.isPinned) excess -= 1 else kept += entry
        }
        return kept.asReversed()
    }

    /** Creates stores in the app-private directory excluded from Android backup. */
    companion object {
        /** Maximum number of entries when enough unpinned entries can be evicted. */
        const val Limit = 50
        private const val DirectoryName = "Clipboard"
        private const val FileName = "clipboard.json"

        /** Creates a store rooted in [Context.getNoBackupFilesDir]. */
        fun from(context: Context, expiry: ClipboardExpiry = ClipboardExpiry.Default) =
            ClipboardHistoryStore(directory(context), expiry)

        internal fun directory(context: Context): File =
            context.applicationContext.noBackupFilesDir.resolve(DirectoryName)
    }
}

private object ClipboardStoreLocks {
    private val locks = ConcurrentHashMap<String, Any>()

    fun forFile(file: File): Any {
        val path = runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
        return locks.computeIfAbsent(path) { Any() }
    }
}
