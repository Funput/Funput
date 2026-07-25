package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId

/**
 * Editable state for a user-created keyboard theme.
 *
 * The draft carries a complete [theme] rather than a delta against its base, because the editor
 * needs to express every token independently. [baseThemeId] is kept only so the editor can offer
 * "restore the original theme" and so deleting a theme can fall back to something sensible; it no
 * longer feeds the saved tokens, so later changes to a built-in theme do not reach themes already
 * derived from it.
 */
data class CustomThemeDraft(
    val theme: KeyboardTheme,
    val name: String = "",
    val author: String = DefaultAuthor,
    val baseThemeId: KeyboardThemeId = KeyboardThemeId.Default,
    val backgroundImage: KeyboardThemeBackgroundImage? = null,
) {
    fun normalizedName(): String = name.normalizedText()

    fun normalizedAuthor(): String = author.normalizedText()

    companion object {
        const val DefaultAuthor = "Funput User"
    }
}

private fun String.normalizedText(): String =
    trim().replace(Regex("\\s+"), " ")
