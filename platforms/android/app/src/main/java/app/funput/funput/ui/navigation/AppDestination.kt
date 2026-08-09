package app.funput.funput.ui.navigation

/**
 * Top-level destinations currently owned by the Funput app shell.
 *
 * [depth] is how far a destination sits from the root. It is what tells a transition whether it is
 * animating forward or back, so it is declared rather than read off the enum order.
 */
internal enum class AppDestination(val depth: Int) {
    SETTINGS(depth = 0),
    THEME_GALLERY(depth = 1),
    CREATE_CUSTOM_THEME(depth = 2),
}
