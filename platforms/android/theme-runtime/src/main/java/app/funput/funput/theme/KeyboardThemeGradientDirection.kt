package app.funput.funput.theme

/**
 * The axis the keyboard background gradient runs along.
 *
 * Values are fractions of the surface, so the renderer can turn them into shader endpoints
 * without knowing the keyboard's size.
 */
enum class KeyboardThemeGradientDirection(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    HORIZONTAL(0f, 0.5f, 1f, 0.5f),
    VERTICAL(0.5f, 0f, 0.5f, 1f),
    DIAGONAL_DOWN(0f, 0f, 1f, 1f),
    DIAGONAL_UP(1f, 0f, 0f, 1f),
    ;

    companion object {
        val Default: KeyboardThemeGradientDirection = DIAGONAL_DOWN

        fun parseOrDefault(value: String?): KeyboardThemeGradientDirection =
            entries.firstOrNull { direction -> direction.name == value } ?: Default
    }
}
