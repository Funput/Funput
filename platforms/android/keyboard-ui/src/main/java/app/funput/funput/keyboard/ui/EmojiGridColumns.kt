package app.funput.funput.keyboard.ui

internal object EmojiGridColumns {
    fun forWidth(widthDp: Float): Int = when {
        widthDp < 360f -> 9
        widthDp < 600f -> 10
        widthDp < 840f -> 12
        else -> 14
    }
}
