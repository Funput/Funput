package app.funput.funput.keyboard.ui.kaomoji.catalog

import android.content.Context
import android.graphics.Paint
import android.os.Build
import androidx.core.content.edit

internal class KaomojiGlyphSupport(context: Context) {
    private val preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)

    fun filter(catalog: KaomojiCatalog, hasGlyph: (String) -> Boolean): KaomojiCatalog {
        if (catalog.items.isEmpty()) return catalog
        val signature = "${Build.FINGERPRINT}|${Build.VERSION.SDK_INT}|${catalog.version}"
        val cached = preferences.getStringSet(UnsupportedKey, null)
            ?.takeIf { preferences.getString(SignatureKey, null) == signature }
        val unsupported = cached ?: catalog.items.asSequence()
            .filterNot { item -> supports(item.text, hasGlyph) }
            .map(KaomojiItem::text)
            .toSet()
            .also {
                preferences.edit {
                    putString(SignatureKey, signature)
                    putStringSet(UnsupportedKey, it)
                }
            }
        return catalog.copy(items = catalog.items.filterNot { it.text in unsupported })
    }

    private fun supports(text: String, hasGlyph: (String) -> Boolean): Boolean =
        text.codePoints().allMatch { codePoint ->
            ignorable(codePoint) || hasGlyph(String(Character.toChars(codePoint)))
        }

    private fun ignorable(codePoint: Int): Boolean {
        if (Character.isWhitespace(codePoint)) return true
        return Character.getType(codePoint) in IgnorableTypes
    }

    private companion object {
        const val FileName = "funput_kaomoji_glyph_support"
        const val SignatureKey = "catalog_device_signature"
        const val UnsupportedKey = "unsupported_kaomoji"
        val IgnorableTypes = setOf(
            Character.FORMAT.toInt(),
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
        )
    }
}
