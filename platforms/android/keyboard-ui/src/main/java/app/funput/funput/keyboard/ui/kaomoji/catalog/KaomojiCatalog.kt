package app.funput.funput.keyboard.ui.kaomoji.catalog

internal enum class KaomojiCategory(val wireName: String, val label: String) {
    RECENT("recent", "Dùng gần đây"),
    HAPPY("happy", "Vui vẻ"),
    LOVE("love", "Yêu thương"),
    SAD("sad", "Buồn bã"),
    ANGRY("angry", "Giận dữ"),
    SURPRISED("surprised", "Ngạc nhiên"),
    CONFUSED("confused", "Bối rối"),
    ACTION("action", "Hành động"),
    ANIMAL("animal", "Động vật"),
    GREETING("greeting", "Chào hỏi");

    companion object {
        fun fromWireName(value: String): KaomojiCategory? =
            entries.firstOrNull { it.wireName == value }
    }
}

internal data class KaomojiItem(
    val text: String,
    val name: String,
    val category: KaomojiCategory,
)

internal data class KaomojiCatalog(
    val version: String,
    val items: List<KaomojiItem>,
) {
    fun items(category: KaomojiCategory): List<KaomojiItem> =
        items.filter { it.category == category }

    companion object {
        val Empty = KaomojiCatalog("", emptyList())
    }
}
