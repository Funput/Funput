package app.funput.funput.ui.settings.dictionary

import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ime.suggestions.dictionary.DictionarySort
import app.funput.funput.ui.settings.dictionary.model.PersonalDictionaryScreenModel
import app.funput.funput.ui.settings.dictionary.model.screenPage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalDictionaryScreenModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val store = DictionaryTestStore(learned("được", 9), learned("không", 3), added("Funput"), blocked("đm"))
    private val model = PersonalDictionaryScreenModel(store, scope, searchPauseMillis = 40L)

    @After fun close() = scope.cancel()

    @Test fun `a load brings the chosen list and every list's count`() {
        model.reload()

        assertTrue(model.hasLoaded)
        assertEquals(listOf("được", "không"), model.page.words.map { it.text })
        assertEquals(2, model.page.count(DictionaryFilter.LEARNED))
        assertEquals(1, model.page.count(DictionaryFilter.BLOCKED))
        assertEquals(9, model.highestUses)
    }

    @Test fun `switching lists or sorts asks again, and the same choice does not`() {
        model.reload()
        model.select(DictionaryFilter.ADDED)
        model.select(DictionaryFilter.ADDED)
        model.sortBy(DictionarySort.ALPHABETICAL)

        assertEquals(3, store.queries.size)
        assertEquals(listOf("Funput"), model.page.words.map { it.text })
        assertEquals(DictionarySort.ALPHABETICAL, store.queries.last().sort)
    }

    @Test fun `a list asked for before the last one landed wins`() {
        store.gate = CompletableDeferred()
        val held = store.gate!!
        model.reload()
        model.select(DictionaryFilter.BLOCKED)
        held.complete(Unit)

        assertEquals(listOf("đm"), model.page.words.map { it.text })
    }

    @Test fun `typing waits for a pause before searching`() {
        model.reload()
        listOf("d", "du", "duo").forEach(model::search)
        waitFor { store.queries.size == 2 }
        Thread.sleep(80)

        assertEquals(listOf("", "duo"), store.queries.map { it.search })
        assertEquals(listOf("được"), model.page.words.map { it.text })
    }

    @Test fun `an applied edit reads the lists again`() = runBlocking {
        model.reload()
        val outcome = model.apply(DictionaryEditKind.BLOCK, "không")

        assertEquals(DictionaryEditOutcome.APPLIED, outcome)
        assertEquals(listOf("được"), model.page.words.map { it.text })
        assertEquals(2, model.page.count(DictionaryFilter.BLOCKED))
    }

    @Test fun `a store that cannot be reached is reported, not thrown`() = runBlocking {
        store.failsEdit = true
        assertNull(model.apply(DictionaryEditKind.ADD, "mới"))
        assertTrue(model.actionFailed)

        store.failsLoad = true
        model.reload()
        assertTrue(model.loadFailed)
        store.failsLoad = false
        model.reload()
        assertFalse(model.loadFailed)
    }

    @Test fun `a pending wipe hides the learned and added words but not the blocked ones`() {
        model.reload()
        val waiting = model.screenPage(resetPending = true)

        assertTrue(waiting.words.isEmpty())
        assertEquals(0, waiting.total)
        assertEquals(1, waiting.count(DictionaryFilter.BLOCKED))
        assertEquals(3, model.screenPage(resetPending = false).total)
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            Thread.sleep(5)
        }
    }
}
