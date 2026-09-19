package app.funput.funput.ime.shortcuts

import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStoring
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeShortcutsControllerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After fun close() = scope.cancel()

    @Test fun `a newer activation discards a stale load`() {
        val store = BlockingStore()
        val received = mutableListOf<ShortcutLibrary>()
        val controller = ImeShortcutsController(scope, store, begin = {}, receive = received::add)

        controller.activate()
        assertTrue(store.started.await(2, TimeUnit.SECONDS))
        controller.activate()
        store.firstRelease.countDown()
        store.secondRelease.countDown()
        waitFor { received.size == 1 }

        assertEquals(listOf("second"), received.single().entries.map { it.trigger })
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue("Timed out", condition())
    }
}

private class BlockingStore : ShortcutsStoring {
    private var loads = 0
    val started = CountDownLatch(1)
    val firstRelease = CountDownLatch(1)
    val secondRelease = CountDownLatch(1)

    override fun load(): ShortcutLibrary {
        val index = synchronized(this) { loads += 1; loads }
        started.countDown()
        if (index == 1) firstRelease.await() else secondRelease.await()
        return ShortcutLibrary(entries = listOf(
            TextShortcut(trigger = if (index == 1) "first" else "second", expansion = "x"),
        ))
    }

    override fun save(library: ShortcutLibrary) = Unit
}
