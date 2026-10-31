package app.funput.funput.ime.suggestions.dictionary

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.suggestions.PersonalSuggestionEngine
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app's side of the store against the real engine: edits are posted, a fresh read shows
 * them at once, and the keyboard applies them when it next drains its inbox.
 */
@RunWith(AndroidJUnit4::class)
class PersonalDictionaryStoreInstrumentedTest {
    private val directory = File(
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "dictionary-${System.nanoTime()}",
    ).apply { mkdirs() }
    private val store = FilePersonalDictionaryStore(directory)

    @After fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun postedEditsShowBeforeTheKeyboardAppliesThem() = runBlocking {
        assertEquals(DictionaryEditOutcome.APPLIED, store.apply(DictionaryEditKind.ADD, "Funput"))
        assertEquals(DictionaryEditOutcome.APPLIED, store.apply(DictionaryEditKind.BLOCK, "đm"))

        val added = store.load(DictionaryQuery(filter = DictionaryFilter.ADDED))
        assertEquals(listOf("funput"), added.words.map { it.text })
        assertEquals(1, added.count(DictionaryFilter.BLOCKED))
        val blocked = store.load(DictionaryQuery(filter = DictionaryFilter.BLOCKED, search = "DM"))
        assertEquals(listOf("đm"), blocked.words.map { it.text })
    }

    @Test
    fun theEngineRefusesWhatTheKeyboardWouldRefuse() = runBlocking {
        assertEquals(DictionaryEditOutcome.INVALID, store.apply(DictionaryEditKind.ADD, "hai từ"))
        assertEquals(DictionaryEditOutcome.APPLIED, store.apply(DictionaryEditKind.ADD, "Funput"))
        assertEquals(DictionaryEditOutcome.UNCHANGED, store.apply(DictionaryEditKind.ADD, "Funput"))
        assertTrue(directory.walk().none { it.name.contains("hai") })
    }

    @Test
    fun theKeyboardDrainsWhatTheAppPosted() {
        runBlocking { store.apply(DictionaryEditKind.ADD, "Funput") }
        val engine = requireNotNull(PersonalSuggestionEngine.open(directory))
        engine.use {
            // Opening drains what was posted while the keyboard was closed.
            assertEquals(listOf("funput"), it.query("fun"))
            assertEquals(0, it.applyPendingEdits())
            // An edit posted while it is open waits for the next field to start.
            runBlocking { store.apply(DictionaryEditKind.BLOCK, "funput") }
            assertEquals(listOf("funput"), it.query("fun"))
            assertEquals(1, it.applyPendingEdits())
            assertEquals(emptyList<String>(), it.query("fun"))
        }
    }
}
