package app.funput.funput.ime.clipboard.persistence

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardHistoryStoreBehaviorTest {
    @Test
    fun `recording is newest first and deduplicates exact text`() {
        val store = store()
        store.record(clipboardEntry("một", sourceToken = "1"), ClipboardEpoch)
        store.record(clipboardEntry("hai", sourceToken = "2"), ClipboardEpoch)
        store.record(clipboardEntry("một", sourceToken = "3"), ClipboardEpoch)
        store.record(clipboardEntry(" một ", sourceToken = "4"), ClipboardEpoch)

        assertEquals(listOf(" một ", "một", "hai"), store.load(ClipboardEpoch).map { it.text })
        assertEquals("3", store.load(ClipboardEpoch)[1].sourceToken)
    }

    @Test
    fun `expiry boundary removes unpinned and preserves pinned entries`() {
        val store = store()
        store.record(clipboardEntry("tạm", sourceToken = "1"), ClipboardEpoch)
        store.record(clipboardEntry("ghim", pinned = true, sourceToken = "2"), ClipboardEpoch)
        val expiry = ClipboardEpoch.plus(ClipboardExpiry.HOUR.duration)

        assertEquals(2, store.load(expiry.minusMillis(1)).size)
        assertEquals(listOf("ghim"), store.load(expiry).map { it.text })
        assertEquals(listOf("ghim"), store.load(expiry.plusMillis(1)).map { it.text })
    }

    @Test
    fun `retention comes from each store instance`() {
        val directory = temporaryClipboardDirectory()
        ClipboardHistoryStore(directory, ClipboardExpiry.WEEK).record(
            clipboardEntry("tạm", sourceToken = "1"),
            ClipboardEpoch,
        )
        val later = ClipboardEpoch.plus(Duration.ofHours(2))

        assertTrue(ClipboardHistoryStore(directory, ClipboardExpiry.HOUR).load(later).isEmpty())
        assertEquals(1, ClipboardHistoryStore(directory, ClipboardExpiry.WEEK).load(later).size)
    }

    @Test
    fun `capacity evicts oldest unpinned item and retains pinned item`() {
        val store = store()
        store.record(clipboardEntry("ghim", pinned = true, sourceToken = "p"), ClipboardEpoch)
        repeat(ClipboardHistoryStore.Limit + 1) { index ->
            store.record(clipboardEntry("mục$index", sourceToken = "$index"), ClipboardEpoch)
        }

        val stored = store.load(ClipboardEpoch)
        assertEquals(ClipboardHistoryStore.Limit, stored.size)
        assertTrue(stored.any { it.text == "ghim" })
        assertFalse(stored.any { it.text == "mục0" })
    }

    @Test
    fun `pinned entries may exceed the normal capacity`() {
        val store = store()
        repeat(ClipboardHistoryStore.Limit + 1) { index ->
            store.record(
                clipboardEntry("ghim$index", pinned = true, sourceToken = "$index"),
                ClipboardEpoch,
            )
        }

        assertEquals(ClipboardHistoryStore.Limit + 1, store.load(ClipboardEpoch).size)
    }

    @Test
    fun `pin remove clear and source token survive reload as specified`() {
        val directory = temporaryClipboardDirectory()
        val entry = clipboardEntry("bí mật", sourceToken = "source-77")
        val store = ClipboardHistoryStore(directory)
        store.record(entry, ClipboardEpoch)
        store.setPinned(true, entry.id, ClipboardEpoch)
        assertTrue(ClipboardHistoryStore(directory).load(ClipboardEpoch).single().isPinned)

        store.remove(entry.id, ClipboardEpoch)
        assertTrue(store.load(ClipboardEpoch).isEmpty())
        assertEquals("source-77", store.lastCapturedSourceToken())
        store.clear()
        assertNull(store.lastCapturedSourceToken())
        assertTrue(store.load(ClipboardEpoch).isEmpty())
    }

    private fun store() = ClipboardHistoryStore(temporaryClipboardDirectory())
}
