package app.funput.funput.ime.clipboard.capture

import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardSystemCaptureTest {
    @Test
    fun `records non-sensitive text when capture is enabled`() = runBlocking {
        val fixture = ClipboardSystemCaptureFixture(capture = true)
        yield()
        assertTrue(fixture.gateway.observing)
        fixture.gateway.notifyChanged()
        yield()
        assertEquals(listOf("hello"), fixture.store.load().map { it.text })
        assertEquals(1, fixture.recordedCount)
        fixture.capture.close()
    }

    @Test
    fun `skips sensitive clips`() = runBlocking {
        val fixture = ClipboardSystemCaptureFixture(capture = true, sensitive = true)
        yield()
        fixture.gateway.notifyChanged()
        yield()
        assertTrue(fixture.store.load().isEmpty())
        fixture.capture.close()
    }

    @Test
    fun `does not observe when capture is off`() = runBlocking {
        val fixture = ClipboardSystemCaptureFixture(capture = false)
        yield()
        assertFalse(fixture.gateway.observing)
        fixture.capture.close()
    }

    @Test
    fun `pauses capture in a password editor and resumes after stop`() = runBlocking {
        val fixture = ClipboardSystemCaptureFixture(capture = true)
        yield()
        fixture.capture.start(KeyboardEditorMode.PASSWORD)
        yield()
        assertFalse(fixture.gateway.observing)
        fixture.gateway.notifyChanged()
        yield()
        assertTrue(fixture.store.load().isEmpty())
        fixture.capture.stop()
        yield()
        assertTrue(fixture.gateway.observing)
        fixture.capture.close()
    }

    @Test
    fun `disabling capture while reading prevents record`() {
        val executor = Executors.newSingleThreadExecutor()
        val io = executor.asCoroutineDispatcher()
        try {
            val enteredRead = CountDownLatch(1)
            val releaseRead = CountDownLatch(1)
            val fixture = ClipboardSystemCaptureFixture(capture = true, ioDispatcher = io)
            runBlocking { yield() }
            assertTrue(fixture.gateway.observing)
            fixture.gateway.blockRead = {
                enteredRead.countDown()
                assertTrue(releaseRead.await(2, TimeUnit.SECONDS))
            }
            fixture.gateway.notifyChanged()
            assertTrue(enteredRead.await(2, TimeUnit.SECONDS))
            fixture.preferences.value = ClipboardPreferences.Default.copy(captureSystemCopies = false)
            releaseRead.countDown()
            Thread.sleep(200)
            assertTrue(fixture.store.load().isEmpty())
            assertEquals(0, fixture.recordedCount)
            fixture.capture.close()
        } finally {
            io.close()
            executor.shutdownNow()
        }
    }
}
