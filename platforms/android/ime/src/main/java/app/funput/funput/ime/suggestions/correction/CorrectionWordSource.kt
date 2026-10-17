package app.funput.funput.ime.suggestions.correction

import java.util.Locale

/**
 * [CorrectionDictionary] for the live keyboard: a word is the user's when they kept it,
 * when they type it often, or when the shipped lists know it.
 *
 * Kept words survive the session through [store], written only while [persists] allows it
 * (a field that asks for no personalised learning keeps them in memory), and are dropped
 * with the personal suggestions when the user resets those.
 */
internal class CorrectionWordSource(
    private val lexicon: CorrectionLexicon,
    private val counts: CorrectionWordCounts,
    private val store: TypoKeptWordsStore? = null,
    private val persists: () -> Boolean = { true },
) : CorrectionDictionary, AutoCloseable {
    private val kept = LinkedHashSet<String>()
    private var loaded = store == null
    private var resetToken: String? = null
    private var preferenceToken: String? = null

    init {
        store?.load(::restore)
    }

    override fun recognizes(word: String): Boolean {
        if (word.lowercase(Locale.ROOT) in kept || counts.isFamiliar(word)) return true
        return lexicon.isKnown(word) ?: true
    }

    override fun keep(word: String) {
        add(word)
        if (loaded && persists()) save()
    }

    override fun weight(word: String): Int = counts.weight(word)

    override fun learn(word: String) = counts.learn(word)

    /** The personal-suggestion reset token from settings; a new one clears what was kept. */
    fun applyResetToken(token: String?) {
        preferenceToken = token
        if (loaded && token != null && token != resetToken) clearForReset(token)
    }

    override fun close() = lexicon.close()

    private fun restore(value: KeptWords) {
        loaded = true
        resetToken = value.resetToken
        val token = preferenceToken
        if (token != null && token != value.resetToken) return clearForReset(token)
        val session = kept.toList()
        kept.clear()
        (value.words + session).forEach(::add)
        if (session.isNotEmpty() && persists()) save()
    }

    private fun add(word: String) {
        val key = word.lowercase(Locale.ROOT).ifEmpty { return }
        kept.remove(key)
        kept.add(key)
        while (kept.size > KeptLimit) kept.remove(kept.first())
    }

    private fun clearForReset(token: String) {
        kept.clear()
        counts.clear()
        resetToken = token
        save()
    }

    private fun save() {
        store?.save(KeptWords(resetToken, kept.toList()))
    }

    internal companion object {
        const val KeptLimit = 200
    }
}
