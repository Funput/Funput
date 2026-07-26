package app.funput.funput.keyboard.ui.emoji

import org.json.JSONObject

internal object EmojiCatalogDecoder {
    fun decode(json: String): EmojiCatalog = runCatching {
        val root = JSONObject(json)
        val values = root.getJSONArray("emojis")
        val emojis = buildList(values.length()) {
            repeat(values.length()) { index ->
                decodeItem(values.getJSONObject(index))?.let(::add)
            }
        }
        EmojiCatalog(
            version = root.optString("version"),
            annotationVersion = root.optString("annotationVersion"),
            emojis = emojis,
        )
    }.getOrDefault(EmojiCatalog.Empty)

    private fun decodeItem(value: JSONObject): EmojiItem? {
        val category = EmojiCategory.fromWireName(value.optString("category")) ?: return null
        val glyph = value.optString("glyph")
        val name = value.optString("name")
        if (glyph.isBlank() || name.isBlank()) return null
        val terms = value.optJSONArray("searchTerms")
        return EmojiItem(
            glyph = glyph,
            name = name,
            localizedName = value.optString("localizedName").takeIf(String::isNotBlank),
            searchTerms = buildList {
                if (terms != null) repeat(terms.length()) { add(terms.optString(it)) }
            },
            category = category,
        )
    }
}
