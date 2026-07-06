package app.funput.funput.theme

/** Metadata and resolved visual tokens for an available keyboard theme. */
data class KeyboardThemeDescriptor(
    val id: KeyboardThemeId,
    val version: Int,
    val name: String,
    val author: String,
    val theme: KeyboardTheme,
) {
    init {
        require(version > 0) { "Theme version must be positive" }
        require(name.isNotBlank()) { "Theme name must not be blank" }
        require(author.isNotBlank()) { "Theme author must not be blank" }
    }
}
