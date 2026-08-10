package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeClipboardControllerRaceTest {
    @Test
    fun `double tap reports busy and commits once`() {
        val fixture = RaceFixture()
        fixture.start()
        val results = mutableListOf<ClipboardPasteResult>()
        val completed = CountDownLatch(1)
        fixture.controller.pasteCurrent { results += it; completed.countDown() }
        assertTrue(fixture.gateway.readStarted.await(2, TimeUnit.SECONDS))
        fixture.controller.pasteCurrent(results::add)
        fixture.gateway.releaseRead.countDown()
        assertTrue(completed.await(2, TimeUnit.SECONDS))

        assertEquals(listOf(ClipboardPasteResult.BUSY, ClipboardPasteResult.PASTED), results)
        assertEquals(listOf("clipboard"), fixture.committed)
        fixture.close()
    }

    @Test
    fun `stop while reading prevents commit`() {
        val fixture = RaceFixture()
        fixture.start()
        fixture.controller.pasteCurrent()
        assertTrue(fixture.gateway.readStarted.await(2, TimeUnit.SECONDS))
        fixture.controller.stop()
        fixture.gateway.releaseRead.countDown()
        fixture.awaitIoIdle()

        assertTrue(fixture.committed.isEmpty())
        assertTrue(fixture.store.load().isEmpty())
        fixture.close()
    }
}

private class RaceFixture {
    private val executor = Executors.newSingleThreadExecutor()
    private val ioDispatcher = executor.asCoroutineDispatcher()
    private val directory = Files.createTempDirectory("clipboard-race").toFile()
    val gateway = BlockingGateway()
    val store = ClipboardHistoryStore(directory)
    val committed = mutableListOf<String>()
    val controller = ImeClipboardController(
        parentScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        preferences = MutableStateFlow(ClipboardPreferences.Default),
        gateway = gateway,
        storeFactory = { ClipboardHistoryStore(directory, it) },
        commitText = committed::add,
        afterCommit = {},
        ioDispatcher = ioDispatcher,
    )

    fun start() {
        controller.start(KeyboardEditorMode.TEXT)
        runBlocking { controller.offer.first { it != null } }
    }

    fun awaitIoIdle() {
        val idle = CountDownLatch(1)
        executor.execute(idle::countDown)
        assertTrue(idle.await(2, TimeUnit.SECONDS))
    }

    fun close() {
        controller.close()
        ioDispatcher.close()
    }
}

private class BlockingGateway : ClipboardGateway {
    val readStarted = CountDownLatch(1)
    val releaseRead = CountDownLatch(1)
    override fun snapshot() = ClipboardSnapshot(ClipboardContentKind.TEXT, false, Token)
    override fun observe(onChanged: () -> Unit) = ClipboardObservation {}
    override fun readText(maxLength: Int): ClipboardReadResult {
        readStarted.countDown()
        releaseRead.await(2, TimeUnit.SECONDS)
        return ClipboardReadResult.Success("clipboard", ClipboardContentKind.TEXT, false, Token)
    }

    private companion object { const val Token = "android:v1:timestamp:1" }
}
