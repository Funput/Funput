package app.funput.funput.ui.settings.search

import java.text.Normalizer
import java.util.Locale

/**
 * Matches a query against settings with or without Vietnamese accents: "dat dau", "đặt dấu" and
 * "DAT DAU" all find "Kiểu đặt dấu". Every word of the query has to appear somewhere in an
 * entry's text, in any order.
 */
internal object SettingsSearchMatcher {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")

    /**
     * Folds [text] to plain lowercase letters. "đ" is mapped by hand: Unicode treats it as a letter
     * of its own, not a "d" with a mark, so decomposition alone leaves it as it is.
     */
    fun normalize(text: String): String {
        val plain = text.replace('đ', 'd').replace('Đ', 'D')
        return Normalizer.normalize(plain, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase(Locale.ROOT)
    }

    /** The folded words of [query]; empty when it holds nothing but spaces. */
    fun words(query: String): List<String> = normalize(query).split(whitespace).filter { it.isNotEmpty() }

    /** Whether every one of [words] occurs in [haystack], which must already be normalized. */
    fun matches(words: List<String>, haystack: String): Boolean =
        words.isNotEmpty() && words.all { word -> word in haystack }
}
