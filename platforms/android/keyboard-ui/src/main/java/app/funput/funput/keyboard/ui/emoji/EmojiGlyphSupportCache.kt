package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import android.os.Build
import androidx.core.content.edit

internal class EmojiGlyphSupportCache(context: Context) {
    private val preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)

    fun filter(catalog: EmojiCatalog, hasGlyph: (String) -> Boolean): EmojiCatalog {
        if (catalog.emojis.isEmpty()) return catalog
        val signature = EmojiCatalogFingerprint.from(catalog, Build.FINGERPRINT, Build.VERSION.SDK_INT)
        val cachedUnsupported = preferences.getStringSet(UnsupportedKey, null)
            ?.takeIf { preferences.getString(SignatureKey, null) == signature }
        val unsupported = cachedUnsupported ?: catalog.emojis
            .asSequence()
            .filterNot { hasGlyph(it.glyph) }
            .map(EmojiItem::glyph)
            .toSet()
            .also {
                preferences.edit {
                    putString(SignatureKey, signature)
                    putStringSet(UnsupportedKey, it)
                }
            }
        return catalog.copy(emojis = catalog.emojis.filterNot { it.glyph in unsupported })
    }

    private companion object {
        const val FileName = "funput_emoji_glyph_support"
        const val SignatureKey = "catalog_device_signature"
        const val UnsupportedKey = "unsupported_glyphs"
    }
}

internal object EmojiCatalogFingerprint {
    fun from(catalog: EmojiCatalog, device: String, sdk: Int): String {
        val glyphHash = catalog.emojis.fold(1) { hash, item -> 31 * hash + item.glyph.hashCode() }
        return "$device|$sdk|${catalog.version}|${catalog.annotationVersion}|$glyphHash"
    }
}
