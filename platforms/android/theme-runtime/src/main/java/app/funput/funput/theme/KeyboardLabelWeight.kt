package app.funput.funput.theme

/**
 * How heavy the key labels are drawn.
 *
 * A short scale of named steps rather than a numeric weight: the platform fonts available on every
 * supported API level come in exactly these four, so a slider would mostly land between them.
 */
enum class KeyboardLabelWeight {
    LIGHT,
    REGULAR,
    MEDIUM,
    BOLD,
    ;

    /**
     * One step heavier, stopping at [BOLD].
     *
     * Modifier keys are drawn one step above the letters so they keep reading as a different kind
     * of key whatever weight is chosen; at the default that is the regular/medium pair the keyboard
     * has always used.
     */
    fun heavier(): KeyboardLabelWeight = entries[minOf(ordinal + 1, entries.lastIndex)]

    companion object {
        val Default: KeyboardLabelWeight = REGULAR

        fun parseOrDefault(value: String?): KeyboardLabelWeight =
            entries.firstOrNull { weight -> weight.name == value } ?: Default
    }
}
