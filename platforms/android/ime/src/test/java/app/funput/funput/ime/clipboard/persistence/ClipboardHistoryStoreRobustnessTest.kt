package app.funput.funput.ime.clipboard.persistence

import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardHistoryStoreRobustnessTest {
    @Test
    fun `missing corrupt unknown and invalid payloads load as empty`() {
        val directory = temporaryClipboardDirectory()
        val store = ClipboardHistoryStore(directory)
        assertTrue(store.load(ClipboardEpoch).isEmpty())

        val file = directory.resolve("clipboard.json")
        listOf(
            "not-json",
            """{"version":2,"lastCapturedSourceToken":null,"items":[]}""",
            """{"version":1,"lastCapturedSourceToken":null,"items":[{}]}""",
        ).forEach { content ->
            file.writeText(content)
            assertTrue(store.load(ClipboardEpoch).isEmpty())
        }
    }

    @Test
    fun `unavailable directory does not throw or persist`() {
        val parent = temporaryClipboardDirectory()
        val notDirectory = parent.resolve("blocked").apply { writeText("file") }
        val store = ClipboardHistoryStore(notDirectory)

        assertEquals(1, store.record(clipboardEntry("rơi"), ClipboardEpoch).size)
        assertTrue(store.load(ClipboardEpoch).isEmpty())
    }

    @Test
    fun `failed replacement preserves the previous file`() {
        val destination = File(temporaryClipboardDirectory(), "clipboard.json")
        destination.writeText("old")
        val file = AtomicJsonFile(destination) { _, _ -> throw IOException("failed") }

        assertFalse(file.write("new"))
        assertEquals("old", destination.readText())
    }

    @Test
    fun `loading expired entries does not rewrite storage`() {
        val directory = temporaryClipboardDirectory()
        val file = directory.resolve("clipboard.json")
        ClipboardHistoryStore(directory).record(clipboardEntry("tạm"), ClipboardEpoch)
        val storedJson = file.readText()

        val later = ClipboardEpoch.plusSeconds(3_601)
        assertTrue(ClipboardHistoryStore(directory).load(later).isEmpty())
        assertEquals(storedJson, file.readText())
    }

    @Test
    fun `multiple store instances serialize updates by file path`() {
        val directory = temporaryClipboardDirectory()
        val stores = listOf(ClipboardHistoryStore(directory), ClipboardHistoryStore(directory))
        val ready = CountDownLatch(20)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(20)
        val futures = List(20) { index ->
            executor.submit {
                ready.countDown()
                start.await()
                stores[index % stores.size].record(
                    clipboardEntry("mục$index", sourceToken = "$index"),
                    ClipboardEpoch,
                )
            }
        }
        ready.await()
        start.countDown()
        futures.forEach { it.get() }
        executor.shutdown()

        val texts = ClipboardHistoryStore(directory).load(ClipboardEpoch).map { it.text }.toSet()
        assertEquals((0 until 20).map { "mục$it" }.toSet(), texts)
    }
}
