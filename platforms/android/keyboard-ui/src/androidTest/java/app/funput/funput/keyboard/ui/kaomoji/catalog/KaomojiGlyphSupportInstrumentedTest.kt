package app.funput.funput.keyboard.ui.kaomoji.catalog

import android.content.Context
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KaomojiGlyphSupportInstrumentedTest {
    @Test
    fun visibleCatalogContainsOnlyCharactersSupportedByTheDevice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("funput_kaomoji_glyph_support", 0).edit().clear().commit()
        val catalog = context.assets.open("KaomojiCatalog.json").bufferedReader().use {
            KaomojiCatalogDecoder.decode(it.readText())
        }
        val paint = Paint()

        val visible = KaomojiGlyphSupport(context).filter(catalog, paint::hasGlyph)

        assertFalse(visible.items.isEmpty())
        assertTrue(visible.items.all { item -> supports(item.text, paint) })
    }

    private fun supports(text: String, paint: Paint): Boolean = text.codePoints().allMatch {
        Character.isWhitespace(it) || ignored(it) || paint.hasGlyph(String(Character.toChars(it)))
    }

    private fun ignored(codePoint: Int): Boolean = Character.getType(codePoint) in setOf(
        Character.FORMAT.toInt(),
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
    )
}
