package app.funput.funput.keyboard.ui.emoji

enum class EmojiCategory(val wireName: String, val label: String) {
    RECENT("recent", "Gần đây"),
    SMILEYS_PEOPLE("smileys_people", "Mặt cười & con người"),
    ANIMALS_NATURE("animals_nature", "Động vật & thiên nhiên"),
    FOOD_DRINK("food_drink", "Đồ ăn & thức uống"),
    ACTIVITIES("activities", "Hoạt động"),
    TRAVEL_PLACES("travel_places", "Du lịch & địa điểm"),
    OBJECTS("objects", "Đồ vật"),
    SYMBOLS("symbols", "Ký hiệu"),
    FLAGS("flags", "Cờ");

    companion object {
        fun fromWireName(value: String): EmojiCategory? =
            entries.firstOrNull { it.wireName == value }
    }
}

data class EmojiItem(
    val glyph: String,
    val name: String,
    val localizedName: String?,
    val searchTerms: List<String>,
    val category: EmojiCategory,
    val variants: List<String> = emptyList(),
) {
    val accessibilityLabel: String get() = localizedName ?: name
}

data class EmojiCatalog(
    val version: String,
    val annotationVersion: String,
    val emojis: List<EmojiItem>,
) {
    fun items(category: EmojiCategory): List<EmojiItem> =
        emojis.filter { it.category == category }

    companion object {
        val Empty = EmojiCatalog("", "", emptyList())
    }
}
