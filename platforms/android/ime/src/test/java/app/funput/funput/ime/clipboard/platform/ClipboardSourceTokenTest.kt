package app.funput.funput.ime.clipboard.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardSourceTokenTest {
    @Test
    fun `timestamp token is versioned and rejects unavailable values`() {
        assertEquals("android:v1:timestamp:42", ClipboardSourceToken.fromTimestamp(42))
        assertNull(ClipboardSourceToken.fromTimestamp(0))
        assertNull(ClipboardSourceToken.fromTimestamp(-1))
    }

    @Test
    fun `text hash is exact stable and sha256 length`() {
        val first = ClipboardSourceToken.fromText(" Việt,\\\n")
        assertEquals(first, ClipboardSourceToken.fromText(" Việt,\\\n"))
        assertEquals(87, first.length)
        assertNotEquals(first, ClipboardSourceToken.fromText("Việt,\\\n"))
    }

    @Test
    fun `fallback cache enriches matching metadata until invalidated`() {
        val cache = ClipboardFallbackTokenCache()
        val snapshot = ClipboardSnapshot(ClipboardContentKind.TEXT, false, null)
        cache.remember(snapshot, "hash")

        assertEquals("hash", cache.enrich(snapshot).sourceToken)
        assertNull(cache.enrich(snapshot.copy(kind = ClipboardContentKind.LINK)).sourceToken)
        assertNull(cache.enrich(snapshot.copy(isSensitive = true)).sourceToken)
        cache.invalidate()
        assertNull(cache.enrich(snapshot).sourceToken)
    }
}
