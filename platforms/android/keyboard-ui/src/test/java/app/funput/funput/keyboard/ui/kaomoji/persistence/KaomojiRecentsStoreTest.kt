package app.funput.funput.keyboard.ui.kaomoji.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KaomojiRecentsStoreTest {
    @Test
    fun `json preserves punctuation whitespace slashes and unicode`() {
        val values = listOf("¯\\_(ツ)_/¯", "(づ｡◕‿‿◕｡)づ", "hello, world", "  (^-^)  ")

        assertEquals(values, KaomojiRecentsStore.decode(KaomojiRecentsStore.encode(values)))
    }

    @Test
    fun `decode keeps newest unique values and caps history`() {
        val values = (0..35).map { "kaomoji-$it" } + listOf("kaomoji-0")

        val decoded = KaomojiRecentsStore.decode(KaomojiRecentsStore.encode(values))

        assertEquals(KaomojiRecentsStore.Limit, decoded.size)
        assertEquals("kaomoji-0", decoded.first())
        assertEquals(decoded.distinct(), decoded)
    }

    @Test
    fun `corrupt values fall back safely`() {
        assertTrue(KaomojiRecentsStore.decode(null).isEmpty())
        assertTrue(KaomojiRecentsStore.decode("not-json").isEmpty())
    }
}
