package app.funput.funput.keyboard.ui.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiSearchIndexTest {
    private val smile = item("😀", "grinning face", "mặt cười toe toét", "happy", "smile")
    private val tears = item("😂", "face with tears of joy", "mặt cười ra nước mắt", "happy", "tears")
    private val coffee = item("☕", "hot beverage", "cà phê", "coffee", "drink")
    private val index = EmojiSearchIndex(listOf(tears, coffee, smile))

    @Test fun `Vietnamese search ignores accents`() {
        assertEquals(listOf(coffee), index.search("ca phe"))
    }

    @Test fun `English multiple tokens require every token`() {
        assertEquals(listOf(tears), index.search("happy tears"))
    }

    @Test fun `exact name ranks ahead of term matches`() {
        assertEquals(smile, index.search("grinning face").first())
    }

    @Test fun `empty query has no result and limit is honored`() {
        assertTrue(index.search("  ").isEmpty())
        assertEquals(1, index.search("happy", limit = 1).size)
    }

    private fun item(glyph: String, name: String, localized: String, vararg terms: String) =
        EmojiItem(glyph, name, localized, terms.toList(), EmojiCategory.SMILEYS_PEOPLE)
}
