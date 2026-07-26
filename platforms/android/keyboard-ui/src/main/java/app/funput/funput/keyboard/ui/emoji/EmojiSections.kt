package app.funput.funput.keyboard.ui.emoji

internal data class EmojiSection(
    val category: EmojiCategory,
    val items: List<EmojiItem>,
)

internal class EmojiSections(val values: List<EmojiSection>) {
    fun position(category: EmojiCategory): Int {
        var position = 0
        values.forEach { section ->
            if (section.category == category) return position
            position += section.items.size + 1
        }
        return -1
    }

    fun categoryAt(position: Int): EmojiCategory? {
        var cursor = 0
        values.forEach { section ->
            cursor += section.items.size + 1
            if (position < cursor) return section.category
        }
        return values.lastOrNull()?.category
    }

    companion object {
        fun from(catalog: EmojiCatalog, recentGlyphs: List<String>): EmojiSections {
            val lookup = catalog.emojis.associateBy(EmojiItem::glyph)
            return EmojiSections(buildList {
                recentGlyphs.mapNotNull(lookup::get).takeIf(List<EmojiItem>::isNotEmpty)?.let {
                    add(EmojiSection(EmojiCategory.RECENT, it))
                }
                EmojiCategory.entries.drop(1).forEach { category ->
                    catalog.items(category).takeIf(List<EmojiItem>::isNotEmpty)?.let {
                        add(EmojiSection(category, it))
                    }
                }
            })
        }
    }
}
