package app.funput.funput.keyboard.ui.kaomoji.browser

import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCatalog
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCategory
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiItem
import org.junit.Assert.assertEquals
import org.junit.Test

class KaomojiSectionsTest {
    private val happy = KaomojiItem("(^_^)", "cười", KaomojiCategory.HAPPY)
    private val love = KaomojiItem("(♡‿♡)", "yêu", KaomojiCategory.LOVE)
    private val catalog = KaomojiCatalog("test", listOf(happy, love))

    @Test
    fun `recents lead when present and disappear when empty`() {
        val recent = KaomojiSections.from(catalog, listOf(love.text))
        val empty = KaomojiSections.from(catalog, emptyList())

        assertEquals(KaomojiCategory.RECENT, recent.values.first().category)
        assertEquals(listOf(love), recent.values.first().items)
        assertEquals(KaomojiCategory.HAPPY, empty.values.first().category)
    }

    @Test
    fun `positions map to the visible category`() {
        val sections = KaomojiSections.from(catalog, emptyList())

        assertEquals(1, sections.position(KaomojiCategory.LOVE))
        assertEquals(KaomojiCategory.LOVE, sections.categoryAt(1))
    }
}
