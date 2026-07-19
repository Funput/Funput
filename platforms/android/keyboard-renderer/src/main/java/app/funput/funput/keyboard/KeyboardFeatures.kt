package app.funput.funput.keyboard

/** Runtime feature toggles for keyboard UI that are not yet user-configurable. */
object KeyboardFeatures {
    /** Enables host completions and on-device personal suggestions in eligible editors. */
    const val SuggestionsEnabled = true

    /** Keeps the emoji access key in the top toolbar slot. */
    const val EmojiToolbarEnabled = true
}
