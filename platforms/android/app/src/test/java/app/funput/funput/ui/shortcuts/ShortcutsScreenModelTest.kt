package app.funput.funput.ui.shortcuts

import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.shortcuts.persistence.ShortcutsStoring
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsScreenModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After fun close() = scope.cancel()

    @Test fun `search is insensitive while duplicate remains exact`() {
        val lower = TextShortcut(trigger = "vn", expansion = "Việt Nam")
        val model = loadedModel(FakeStore(ShortcutLibrary(entries = listOf(lower))))

        model.query = "NAM"

        assertEquals(listOf(lower), model.filteredEntries)
        assertTrue(model.isDuplicate(TextShortcut(trigger = "vn", expansion = "khác")))
        assertFalse(model.isDuplicate(TextShortcut(trigger = "VN", expansion = "khác")))
    }

    @Test fun `writes are serialized and overlapping mutations are ignored`() {
        val store = FakeStore(ShortcutLibrary()).apply { blockSave = true }
        val model = loadedModel(store)

        model.save(TextShortcut(trigger = "a", expansion = "one")) {}
        assertTrue(store.saveStarted.await(2, TimeUnit.SECONDS))
        model.save(TextShortcut(trigger = "b", expansion = "two")) {}
        store.releaseSave.countDown()
        waitFor { !model.isSaving }

        assertEquals(1, store.saveCount)
        assertEquals(listOf("a"), store.value.entries.map { it.trigger })
    }

    @Test fun `option failure rolls back and retry reload unlocks writes`() {
        val store = FakeStore(ShortcutLibrary()).apply {
            saveError = ShortcutsStorageError.WriteFailed
        }
        val model = loadedModel(store)
        model.updateOptions { it.copy(smartCase = false) }
        waitFor { !model.isSaving }
        assertTrue(model.library.smartCase)
        assertEquals(ShortcutsStorageError.WriteFailed, model.saveError)

        store.saveError = null
        store.loadError = ShortcutsStorageError.ReadFailed
        model.reload()
        waitFor { !model.isLoading }
        assertFalse(model.canWrite)
        store.loadError = null
        model.reload()
        waitFor { !model.isLoading }
        assertTrue(model.canWrite)
    }

    @Test fun `save completion runs only after a successful write`() {
        val store = FakeStore(ShortcutLibrary())
        val model = loadedModel(store)
        var completions = 0

        model.save(TextShortcut(trigger = "ok", expansion = "saved")) { completions += 1 }
        waitFor { !model.isSaving }
        assertEquals(1, completions)

        store.saveError = ShortcutsStorageError.WriteFailed
        model.save(TextShortcut(trigger = "no", expansion = "failure")) { completions += 1 }
        waitFor { !model.isSaving }
        assertEquals(1, completions)
    }

    private fun loadedModel(store: FakeStore): ShortcutsScreenModel =
        ShortcutsScreenModel(store, scope).also { model ->
            model.reload()
            waitFor { !model.isLoading }
        }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue("Timed out", condition())
    }
}

private class FakeStore(initial: ShortcutLibrary) : ShortcutsStoring {
    @Volatile var value = initial
    @Volatile var loadError: Throwable? = null
    @Volatile var saveError: Throwable? = null
    var blockSave = false
    var saveCount = 0
    val saveStarted = CountDownLatch(1)
    val releaseSave = CountDownLatch(1)

    override fun load(): ShortcutLibrary = loadError?.let { throw it } ?: value

    override fun save(library: ShortcutLibrary) {
        saveStarted.countDown()
        if (blockSave) releaseSave.await()
        saveError?.let { throw it }
        value = library
        saveCount += 1
    }
}
