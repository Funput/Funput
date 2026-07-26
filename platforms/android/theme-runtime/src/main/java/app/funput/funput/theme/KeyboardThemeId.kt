package app.funput.funput.theme

/**
 * Stable identifier for a keyboard theme.
 *
 * Theme identifiers are persisted and may eventually come from an installed theme package, so
 * they must not be represented by an enum. The restricted format keeps identifiers portable
 * across storage, package manifests, and backend APIs.
 */
@JvmInline
value class KeyboardThemeId private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        private const val MaxLength = 128
        private val ValidIdentifier = Regex("[a-z0-9]+(?:[._-][a-z0-9]+)*")

        val Dark: KeyboardThemeId = KeyboardThemeId("dark")
        val Light: KeyboardThemeId = KeyboardThemeId("light")
        val GlassDark: KeyboardThemeId = KeyboardThemeId("glass-dark")
        val Default: KeyboardThemeId = Dark

        /** Creates an identifier or throws when [value] is not safe to persist. */
        fun of(value: String): KeyboardThemeId {
            require(isValid(value)) { "Invalid keyboard theme identifier: $value" }
            return KeyboardThemeId(value)
        }

        /** Parses persisted or external data without throwing. */
        fun parseOrNull(value: String?): KeyboardThemeId? =
            value?.takeIf(::isValid)?.let(::KeyboardThemeId)

        private fun isValid(value: String): Boolean =
            value.length in 1..MaxLength && ValidIdentifier.matches(value)
    }
}
