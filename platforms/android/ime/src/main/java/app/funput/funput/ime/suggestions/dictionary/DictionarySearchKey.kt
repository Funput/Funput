package app.funput.funput.ime.suggestions.dictionary

import java.text.Normalizer
import java.util.Locale

/**
 * A word with its case and Vietnamese marks folded away, `đ` included, so `duoc`
 * finds `được` as it does in the engine's own lists. The engine lists blocked
 * words without a search, since there are few of them; this is that search.
 */
class DictionarySearchKey(text: String) {
    private val folded: String = fold(text)

    val isEmpty: Boolean get() = folded.isEmpty()

    fun contains(other: DictionarySearchKey): Boolean = folded.contains(other.folded)

    private companion object {
        val combiningMarks = Regex("\\p{Mn}+")

        fun fold(text: String): String {
            val plain = text.replace('đ', 'd').replace('Đ', 'D')
            return Normalizer.normalize(plain, Normalizer.Form.NFD)
                .replace(combiningMarks, "")
                .lowercase(Locale.ROOT)
        }
    }
}
