package app.funput.funput.keyboard.ui.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EmojiCatalogFingerprintTest {
    private val emoji = EmojiItem("😀", "face", null, emptyList(), EmojiCategory.SMILEYS_PEOPLE)
    private val catalog = EmojiCatalog("15.1", "48.2", listOf(emoji))

    @Test
    fun fingerprintIsStableForSameCatalogAndDevice() {
        assertEquals(
            EmojiCatalogFingerprint.from(catalog, "device", 35),
            EmojiCatalogFingerprint.from(catalog.copy(), "device", 35),
        )
    }

    @Test
    fun fingerprintChangesWithCatalogOrDevice() {
        val baseline = EmojiCatalogFingerprint.from(catalog, "device", 35)
        assertNotEquals(baseline, EmojiCatalogFingerprint.from(catalog, "updated", 35))
        assertNotEquals(baseline, EmojiCatalogFingerprint.from(catalog, "device", 36))
        assertNotEquals(baseline, EmojiCatalogFingerprint.from(catalog.copy(version = "16"), "device", 35))
        assertNotEquals(
            baseline,
            EmojiCatalogFingerprint.from(catalog.copy(emojis = listOf(emoji.copy(glyph = "🙂"))), "device", 35),
        )
    }
}
