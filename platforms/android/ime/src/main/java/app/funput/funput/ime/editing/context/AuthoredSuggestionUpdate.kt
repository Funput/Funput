package app.funput.funput.ime.editing

/** What the token tracker hands the suggestion service after an edit. */
internal data class AuthoredSuggestionUpdate(
    val prefix: String,
    val completedToken: String?,
    /** The word [prefix] is being typed after, when one can be vouched for. */
    val context: String? = null,
    /** The word [context] stands after, when the two stand in a row. */
    val older: String? = null,
    /** Whether [context] was read from the document rather than typed through Funput. */
    val borrowed: Boolean = false,
) {
    /** The words before [prefix] as the engine reads them: oldest first, space separated. */
    val queryContext: String? get() = context?.let { last -> older?.let { "$it $last" } ?: last }

    /**
     * The word a completed token is learned as following. Never a borrowed one: the
     * store learns what was typed through Funput, not what a document already held.
     */
    val learnContext: String? get() = context.takeUnless { borrowed }

    companion object {
        val Empty = AuthoredSuggestionUpdate("", null)
    }
}
