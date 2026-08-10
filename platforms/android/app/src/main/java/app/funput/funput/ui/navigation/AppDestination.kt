package app.funput.funput.ui.navigation

/**
 * The tabs of the app. Each keeps its own back stack, so leaving a tab half way into something and
 * coming back to it lands where you were rather than at its root.
 */
internal enum class TopLevelDestination {
    SETTINGS,
    APPEARANCE,
    ABOUT,
    ;

    companion object {
        /** The tab the app opens on, and the one back falls back to from any other tab. */
        val Start = SETTINGS
    }
}

/**
 * Every screen the app shell owns.
 *
 * [depth] is how far a screen sits from its tab's root, and [tab] is which stack it belongs to.
 * Together they tell a transition what kind of move it is animating: a change of [tab] is lateral,
 * a change of [depth] goes in or out. Both are declared rather than read off the enum order.
 */
internal enum class AppDestination(val tab: TopLevelDestination, val depth: Int) {
    SETTINGS(tab = TopLevelDestination.SETTINGS, depth = 0),
    THEME_GALLERY(tab = TopLevelDestination.APPEARANCE, depth = 0),
    CREATE_CUSTOM_THEME(tab = TopLevelDestination.APPEARANCE, depth = 1),
    ABOUT(tab = TopLevelDestination.ABOUT, depth = 0),
    ;

    companion object {
        /** The screen a tab shows before anything has been pushed onto it. */
        fun rootOf(tab: TopLevelDestination): AppDestination =
            entries.first { destination -> destination.tab == tab && destination.depth == 0 }
    }
}
