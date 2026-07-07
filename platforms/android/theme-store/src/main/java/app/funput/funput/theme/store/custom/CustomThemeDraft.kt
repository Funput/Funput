package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId

/** Editable state for a user-created keyboard theme. */
data class CustomThemeDraft(
    val name: String = "",
    val author: String = DefaultAuthor,
    val baseThemeId: KeyboardThemeId = KeyboardThemeId.Default,
    val backgroundImage: KeyboardThemeBackgroundImage? = null,
    val overrides: CustomThemeOverrides = CustomThemeOverrides(),
) {
    fun normalizedName(): String = name.normalizedText()

    fun normalizedAuthor(): String = author.normalizedText()

    companion object {
        const val DefaultAuthor = "Funput User"
    }
}

private fun String.normalizedText(): String =
    trim().replace(Regex("\\s+"), " ")
