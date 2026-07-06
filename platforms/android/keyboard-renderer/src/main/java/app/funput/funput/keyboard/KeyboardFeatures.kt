package app.funput.funput.keyboard

/** Runtime feature toggles for keyboard UI that are not yet user-configurable. */
object KeyboardFeatures {
    /** Word suggestions UI is deferred; emoji toolbar can still be shown. */
    const val SuggestionsEnabled = false

    /** Keeps the emoji access key in the top toolbar slot. */
    const val EmojiToolbarEnabled = true
}
