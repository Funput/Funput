package app.funput.funput.ime.suggestions

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.suggestions.lexicon.LexiconInstaller
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class LexiconWorkerInstrumentedTest {
    @Test fun attachesOncePerPersistentOrFallbackEngineAndReattachesOnReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "worker-test-${System.nanoTime()}").apply { mkdirs() }
        val attachments = AtomicInteger()
        try {
            for (fallback in listOf(false, true, false)) {
                val results = LinkedBlockingQueue<List<String>>()
                val worker = PersonalSuggestionWorker(
                    storeDirectory = { if (fallback) error("store unavailable") else root.resolve("personal") },
                    attachLexicon = { engine ->
                        assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
                        attachments.incrementAndGet()
                        assertTrue(LexiconInstaller({ context.assets.open("lexicon/en.lex") },
                            { root.resolve("Lexicon") }).attach(engine::attachLexicon))
                    },
                    publish = { _, words -> results.offer(words) },
                )
                try {
                    val count = attachments.get()
                    repeat(3) { generation ->
                        worker.query(PersonalSuggestionRequest("wh", generation.toLong(), 1))
                        assertEquals(listOf("which", "when", "what"), results.poll(20, TimeUnit.SECONDS))
                    }
                    assertTrue(attachments.get() in count..count + 1)
                } finally { val closed = CountDownLatch(1); worker.close { closed.countDown() }; assertTrue(closed.await(20, TimeUnit.SECONDS)) }
            }
            assertEquals(3, attachments.get())
        } finally { root.deleteRecursively() }
    }

    @Test fun missingLexiconStillLearnsAndQueriesPersonalWords() {
        val results = LinkedBlockingQueue<List<String>>()
        val worker = PersonalSuggestionWorker(
            storeDirectory = { error("unavailable") },
            attachLexicon = { throw java.io.IOException("missing") },
            publish = { _, words -> results.offer(words) },
        )
        try {
            repeat(2) { worker.learn("funput", null) }
            worker.query(PersonalSuggestionRequest("fu", 1, 1))
            assertEquals(listOf("funput"), results.poll(20, TimeUnit.SECONDS))
        } finally { val closed = CountDownLatch(1); worker.close { closed.countDown() }; assertTrue(closed.await(20, TimeUnit.SECONDS)) }
    }
}
