package app.funput.funput.keyboard.ui.emoji

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmojiGlyphSupportCacheInstrumentedTest {
    @Test
    fun deviceGlyphChecksAreReusedAfterFirstFilter() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("funput_emoji_glyph_support", 0).edit().clear().commit()
        val supported = item("😀")
        val unsupported = item("missing")
        val catalog = EmojiCatalog("test", "test", listOf(supported, unsupported))
        val cache = EmojiGlyphSupportCache(context)
        var checks = 0

        assertEquals(listOf(supported), cache.filter(catalog) {
            checks++
            it != unsupported.glyph
        }.emojis)
        assertEquals(2, checks)

        checks = 0
        assertEquals(listOf(supported), cache.filter(catalog) {
            checks++
            true
        }.emojis)
        assertEquals(0, checks)
    }

    private fun item(glyph: String) = EmojiItem(
        glyph = glyph,
        name = glyph,
        localizedName = null,
        searchTerms = emptyList(),
        category = EmojiCategory.SMILEYS_PEOPLE,
    )
}
