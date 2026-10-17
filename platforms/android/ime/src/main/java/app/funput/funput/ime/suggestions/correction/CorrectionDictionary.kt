package app.funput.funput.ime.suggestions.correction

/**
 * What typo correction asks about words, answered from the user's own typing and the
 * shipped word lists. Every call runs on the IME main thread, inside one keystroke.
 */
internal interface CorrectionDictionary {
    /**
     * Whether [word] is one the user means: never correct it, and never offer a repair for it.
     * True while the lists are still loading, so nothing is corrected blind.
     */
    fun recognizes(word: String): Boolean

    /** Remember that the user wants [word] as typed: they undid or refused its repair. */
    fun keep(word: String)

    /** How familiar [word] is, as the engine's `uses`: shipped tier plus the user's count. */
    fun weight(word: String): Int = 0

    /** The user just finished typing [word]. */
    fun learn(word: String) = Unit
}
