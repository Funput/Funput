package app.funput.funput.ime.suggestions.correction

import java.util.Locale

/**
 * How often the user typed each word this session, in memory and on the main thread.
 *
 * The personal store already knows these counts, but it lives on the suggestion worker's
 * thread, and correction must answer inside the boundary keystroke without waiting on it.
 */
internal class CorrectionWordCounts(private val syllableWeight: (String) -> Int) {
    private val counts = HashMap<String, Int>()

    fun learn(word: String) {
        val key = word.lowercase(Locale.ROOT).ifEmpty { return }
        if (key !in counts && counts.size >= CountLimit) evictRarest()
        counts[key] = (counts[key] ?: 0).let { if (it == Int.MAX_VALUE) it else it + 1 }
    }

    /** The shipped tier (4 down to 1, 0 when unlisted) plus the user's own count. */
    fun weight(word: String): Int {
        val count = counts[word.lowercase(Locale.ROOT)] ?: 0
        val shipped = syllableWeight(word)
        return if (count > Int.MAX_VALUE - shipped) Int.MAX_VALUE else shipped + count
    }

    /** Seen at least twice: once could itself have been the slip. */
    fun isFamiliar(word: String): Boolean = (counts[word.lowercase(Locale.ROOT)] ?: 0) >= FamiliarCount

    fun clear() = counts.clear()

    private fun evictRarest() {
        counts.minByOrNull { it.value }?.let { counts.remove(it.key) }
    }

    internal companion object {
        const val CountLimit = 2000
        const val FamiliarCount = 2
    }
}
