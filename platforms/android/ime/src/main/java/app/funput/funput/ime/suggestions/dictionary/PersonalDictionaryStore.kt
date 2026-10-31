package app.funput.funput.ime.suggestions.dictionary

import android.content.Context
import app.funput.funput.ime.suggestions.personalSuggestionDirectory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What the dictionary page reads and changes. The file store talks to the engine; tests bring their own. */
interface PersonalDictionaryStoring {
    /** Throws [IOException] when the store cannot be read. */
    suspend fun load(query: DictionaryQuery): DictionaryPage

    /** Throws [IOException] when an edit that applies could not be handed to the keyboard. */
    suspend fun apply(kind: DictionaryEditKind, word: String): DictionaryEditOutcome
}

/**
 * The keyboard's store, read and changed from the settings app.
 *
 * Every load reopens the snapshot, because the keyboard may have learned or drained edits
 * since. An edit is previewed on the open snapshot first, so a word the keyboard would
 * refuse is refused here with the same outcome, and only an edit that applies is posted.
 * Calls run one at a time off the main thread: a snapshot has one owner.
 */
class FilePersonalDictionaryStore internal constructor(private val directory: File) : PersonalDictionaryStoring {
    private val lock = Mutex()
    private var snapshot: PersonalDictionarySnapshot? = null

    override suspend fun load(query: DictionaryQuery): DictionaryPage = locked {
        val opened = reopen()
        DictionaryPage(
            words = opened.words(query),
            counts = DictionaryFilter.entries.associateWith(opened::count),
        )
    }

    override suspend fun apply(kind: DictionaryEditKind, word: String): DictionaryEditOutcome = locked {
        val opened = snapshot ?: reopen()
        val outcome = opened.preview(kind, word)
        if (outcome == DictionaryEditOutcome.APPLIED && !opened.post(kind, word)) {
            // The view now holds an edit the store never will; the next load drops it.
            close()
            throw IOException("Could not post the dictionary edit")
        }
        outcome
    }

    private fun reopen(): PersonalDictionarySnapshot {
        close()
        return (PersonalDictionarySnapshot.open(directory) ?: throw IOException("Could not read the dictionary"))
            .also { snapshot = it }
    }

    private fun close() {
        snapshot?.close()
        snapshot = null
    }

    private suspend fun <T> locked(body: () -> T): T = withContext(Dispatchers.IO) { lock.withLock { body() } }

    companion object {
        fun from(context: Context): FilePersonalDictionaryStore =
            FilePersonalDictionaryStore(personalSuggestionDirectory(context.applicationContext))
    }
}
