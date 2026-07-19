package app.funput.funput.ime.suggestions

import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
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

    @Test
    fun warmQueryMeetsReleaseLatencyAndMemoryBudgets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeFalse(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        onSuggestionThread {
            val engine = requireNotNull(PersonalSuggestionEngine.inMemory())
            engine.use { suggestions ->
                repeat(5_000) { index ->
                    val token = benchmarkToken(index)
                    suggestions.learn(token)
                    suggestions.learn(token)
                }
                repeat(WarmupQueries) { suggestions.query("từ") }
                val samples = LongArray(MeasuredQueries)
                repeat(MeasuredQueries) { index ->
                    val started = System.nanoTime()
                    suggestions.query("từ")
                    samples[index] = System.nanoTime() - started
                }
                samples.sort()
                val stats = suggestions.stats()
                println(
                    "FUNPUT_BENCHMARK p50=${samples[Percentile50] / 1_000.0}µs " +
                        "p95=${samples[Percentile95] / 1_000.0}µs " +
                        "p99=${samples[Percentile99] / 1_000.0}µs heap=${stats.estimatedHeapBytes}",
                )
                assertTrue("p95=${samples[Percentile95] / 1_000}µs", samples[Percentile95] < 100_000)
                assertTrue(stats.estimatedHeapBytes < 4L * 1_024 * 1_024)
            }
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

    private fun benchmarkToken(value: Int): String {
        var remaining = value
        return buildString {
            append("từ")
            repeat(3) {
                append(('a'.code + remaining % 26).toChar())
                remaining /= 26
            }
        }
    }

    private companion object {
        const val WarmupQueries = 1_000
        const val MeasuredQueries = 100_000
        const val Percentile50 = MeasuredQueries * 50 / 100
        const val Percentile95 = MeasuredQueries * 95 / 100
        const val Percentile99 = MeasuredQueries * 99 / 100
    }
}
