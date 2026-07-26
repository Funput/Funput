package app.funput.funput.keyboard.ui.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiRecentsStoreTest {
    @Test fun `AndroidX value keeps order and removes duplicates`() {
        assertEquals(listOf("😂", "😀"), EmojiRecentsStore.decode("😂,😀,😂"))
    }

    @Test fun `corrupt or empty value falls back safely`() {
        assertTrue(EmojiRecentsStore.decode(null).isEmpty())
        assertTrue(EmojiRecentsStore.decode(",,,").isEmpty())
    }

    @Test fun `history is capped at thirty`() {
        val value = (0..40).joinToString(",") { "emoji-$it" }
        assertEquals(30, EmojiRecentsStore.decode(value).size)
    }
}
