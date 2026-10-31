package app.funput.funput.ime.suggestions

import app.funput.funput.ime.settings.ToneStyle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shipped pairs through the real worker and JNI, on a store that has learned nothing. */
class SeedLanguageWorkerInstrumentedTest {
    private val results = LinkedBlockingQueue<List<String>>()
    private var generation = 0L

    @Test fun theSettingReachesTheEngineAndFollowsEveryChange() = withWorker { worker ->
        assertEquals("off until the composer says", emptyList<String>(), predicted(worker, "cảm"))

        worker.setSeedLanguage(SeedLanguage.Vietnamese(ToneStyle.MODERN))
        assertEquals(listOf("ơn"), predicted(worker, "cảm"))
        assertEquals(listOf("khoẻ"), predicted(worker, "sức"))

        worker.setSeedLanguage(SeedLanguage.Vietnamese(ToneStyle.TRADITIONAL))
        assertEquals(listOf("khỏe"), predicted(worker, "sức"))

        worker.setSeedLanguage(SeedLanguage.Off)
        assertEquals(emptyList<String>(), predicted(worker, "cảm"))
    }

    private fun predicted(worker: PersonalSuggestionWorker, previous: String): List<String>? {
        worker.query(PersonalSuggestionRequest("", generation++, 1, context = previous))
        return results.poll(20, TimeUnit.SECONDS)
    }

    private fun withWorker(body: (PersonalSuggestionWorker) -> Unit) {
        // An unavailable store keeps the test off disk: the worker falls back to memory.
        val worker = PersonalSuggestionWorker(
            storeDirectory = { error("unavailable") },
            publish = { _, words -> results.offer(words) },
        )
        try {
            body(worker)
        } finally {
            val closed = CountDownLatch(1)
            worker.close { closed.countDown() }
            assertTrue(closed.await(20, TimeUnit.SECONDS))
        }
    }
}
