package app.funput.funput.keyboard.ui.kaomoji.catalog

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KaomojiCatalogDecoderTest {
    private val canonical = File(
        "../../ios/Packages/FunputKit/Sources/KeyboardRenderer/Resources/KaomojiCatalog.json",
    )

    @Test
    fun `canonical catalog matches the iOS product contract`() {
        val catalog = KaomojiCatalogDecoder.decode(canonical.readText())

        assertEquals("1", catalog.version)
        assertEquals(161, catalog.items.size)
        assertEquals(161, catalog.items.map(KaomojiItem::text).distinct().size)
        assertTrue(catalog.items.all { it.text.isNotBlank() && it.name.isNotBlank() })
        assertFalse(catalog.items.any { it.category == KaomojiCategory.RECENT })
        KaomojiCategory.entries.drop(1).forEach { assertTrue(catalog.items(it).isNotEmpty()) }
    }

    @Test
    fun `category order keeps recents first and love beside happy`() {
        assertEquals(
            listOf(KaomojiCategory.RECENT, KaomojiCategory.HAPPY, KaomojiCategory.LOVE),
            KaomojiCategory.entries.take(3),
        )
    }

    @Test
    fun `malformed json returns an empty catalog`() {
        assertEquals(KaomojiCatalog.Empty, KaomojiCatalogDecoder.decode("{broken"))
    }
}
