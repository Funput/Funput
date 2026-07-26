package app.funput.funput.keyboard.ui.emoji

import java.text.Normalizer
import java.util.Locale

internal class EmojiSearchIndex(items: List<EmojiItem>) {
    private val entries = items.mapIndexed { index, item -> Entry(index, item) }

    fun search(query: String, limit: Int = 64): List<EmojiItem> {
        val words = normalize(query).split(' ').filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        return entries.mapNotNull { entry ->
            rank(entry, words)?.let { rank -> Ranked(rank, entry.index, entry.item) }
        }.sortedWith(compareBy<Ranked> { it.rank }.thenBy { it.index })
            .take(limit)
            .map(Ranked::item)
    }

    private fun rank(entry: Entry, query: List<String>): Int? {
        val phrase = query.joinToString(" ")
        if (entry.names.any { it == phrase }) return 0
        if (entry.names.any { it.startsWith(phrase) }) return 1
        if (query.all { word -> entry.words.any { it.startsWith(word) } }) return 2
        if (query.all { word -> entry.terms.any { it.contains(word) } }) return 3
        return null
    }

    private data class Entry(val index: Int, val item: EmojiItem) {
        val names = listOfNotNull(item.localizedName, item.name).map(::normalize)
        val terms = (item.searchTerms + names).map(::normalize)
        val words = terms.flatMap { it.split(' ') }.filter(String::isNotBlank)
    }

    private data class Ranked(val rank: Int, val index: Int, val item: EmojiItem)

    companion object {
        internal fun normalize(value: String): String = Normalizer
            .normalize(value.lowercase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
