package app.funput.funput.theme

/**
 * Optional background layer for custom themes.
 *
 * The source is intentionally a string so app storage can use a content URI, file URI, or stable
 * theme-package asset reference later without making the renderer depend on Android storage APIs.
 */
data class KeyboardThemeBackgroundImage(
    val source: String,
    val opacity: Float,
) {
    init {
        require(source.isNotBlank()) { "Background image source must not be blank" }
        require(opacity in 0f..1f) { "Background image opacity must be between 0 and 1" }
    }
}
