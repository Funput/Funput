package app.funput.funput.ime.suggestions

import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalSuggestionEngineInstrumentedTest {
    @Test
    fun learnsQueriesAndFailsSilentlyOnOwnerThread() = onSuggestionThread {
        val engine = requireNotNull(PersonalSuggestionEngine.inMemory())
        engine.use {
            assertTrue(it.learn("tiếng"))
            assertTrue(it.learn("tiếng"))
            assertEquals(listOf("tiếng"), it.query("ti"))
            assertEquals(emptyList<String>(), it.query("không-có"))
        }
    }

    private fun onSuggestionThread(block: () -> Unit) {
        val thread = HandlerThread("SuggestionInstrumentedTest").apply { start() }
        val finished = CountDownLatch(1)
        var failure: Throwable? = null
        Handler(thread.looper).post {
            runCatching(block).onFailure { failure = it }
            finished.countDown()
            thread.quitSafely()
        }
        assertTrue(finished.await(30, TimeUnit.SECONDS))
        failure?.let { throw it }
    }
}
