package app.funput.funput.keyboard.ui.kaomoji.browser

import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCatalog
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCategory
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiItem

internal data class KaomojiSection(
    val category: KaomojiCategory,
    val items: List<KaomojiItem>,
)

internal class KaomojiSections(val values: List<KaomojiSection>) {
    fun position(category: KaomojiCategory): Int =
        values.indexOfFirst { it.category == category }

    fun categoryAt(position: Int): KaomojiCategory? =
        values.getOrNull(position)?.category ?: values.lastOrNull()?.category

    companion object {
        fun from(catalog: KaomojiCatalog, recentTexts: List<String>): KaomojiSections {
            val lookup = catalog.items.associateBy(KaomojiItem::text)
            return KaomojiSections(buildList {
                recentTexts.mapNotNull(lookup::get).takeIf(List<KaomojiItem>::isNotEmpty)?.let {
                    add(KaomojiSection(KaomojiCategory.RECENT, it))
                }
                KaomojiCategory.entries.drop(1).forEach { category ->
                    catalog.items(category).takeIf(List<KaomojiItem>::isNotEmpty)?.let {
                        add(KaomojiSection(category, it))
                    }
                }
            })
        }
    }
}
