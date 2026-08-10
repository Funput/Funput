package app.funput.funput.keyboard.ui.kaomoji.catalog

import org.json.JSONObject

internal object KaomojiCatalogDecoder {
    fun decode(json: String): KaomojiCatalog = runCatching {
        val root = JSONObject(json)
        val values = root.getJSONArray("items")
        val items = buildList(values.length()) {
            repeat(values.length()) { index ->
                decodeItem(values.getJSONObject(index))?.let(::add)
            }
        }
        KaomojiCatalog(root.optString("version"), items)
    }.getOrDefault(KaomojiCatalog.Empty)

    private fun decodeItem(value: JSONObject): KaomojiItem? {
        val category = KaomojiCategory.fromWireName(value.optString("category")) ?: return null
        if (category == KaomojiCategory.RECENT) return null
        val text = value.optString("text")
        val name = value.optString("name")
        if (text.isBlank() || name.isBlank()) return null
        return KaomojiItem(text, name, category)
    }
}
