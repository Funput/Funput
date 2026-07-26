package app.funput.funput.keyboard.ui.emoji

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogDecoderTest {
    @Test
    fun `canonical catalog has expected metadata and item count`() {
        val file = File(
            "../../ios/Packages/FunputKit/Sources/KeyboardRenderer/Resources/EmojiCatalog.json",
        )
        val catalog = EmojiCatalogDecoder.decode(file.readText())

        assertEquals("15.1", catalog.version)
        assertEquals("48.2", catalog.annotationVersion)
        assertEquals(1_898, catalog.emojis.size)
        assertTrue(catalog.emojis.any { it.localizedName != null })
        assertEquals(8, catalog.emojis.map(EmojiItem::category).distinct().size)
    }

    @Test
    fun `malformed json returns empty catalog`() {
        assertEquals(EmojiCatalog.Empty, EmojiCatalogDecoder.decode("{broken"))
    }
}
