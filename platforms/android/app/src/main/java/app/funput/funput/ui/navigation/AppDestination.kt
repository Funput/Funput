package app.funput.funput.ui.navigation

/**
 * The tabs of the app, in tab-bar order. Each keeps its own back stack, so leaving a tab half way
 * into something and coming back to it lands where you were rather than at its root.
 *
 * Five tabs is the ceiling, as on iOS, and all five are used: features that come later (an
 * account, sync) belong inside [ME] rather than beside it.
 */
internal enum class TopLevelDestination {
    KEYBOARD,
    SHORTCUTS,
    APPEARANCE,
    NEWS,
    ME,
    ;

    companion object {
        /** The tab the app opens on, and the one back falls back to from any other tab. */
        val Start = KEYBOARD
    }
}

/**
 * Every screen the app shell owns.
 *
 * [depth] is how far a screen sits from its tab's root, and [tab] is which stack it belongs to.
 * Together they tell a transition what kind of move it is animating: a change of [tab] is lateral,
 * a change of [depth] goes in or out. Both are declared rather than read off the enum order.
 *
 * The settings pages are listed once per tab that can open them; `SettingsPage` says which.
 */
internal enum class AppDestination(val tab: TopLevelDestination, val depth: Int) {
    KEYBOARD(tab = TopLevelDestination.KEYBOARD, depth = 0),
    TYPING(tab = TopLevelDestination.KEYBOARD, depth = 1),
    LAYOUT(tab = TopLevelDestination.KEYBOARD, depth = 1),
    SMART_INPUT(tab = TopLevelDestination.KEYBOARD, depth = 1),
    FEEDBACK(tab = TopLevelDestination.KEYBOARD, depth = 1),
    CLIPBOARD(tab = TopLevelDestination.KEYBOARD, depth = 1),
    SPEECH(tab = TopLevelDestination.KEYBOARD, depth = 1),
    HARDWARE_KEYBOARD(tab = TopLevelDestination.KEYBOARD, depth = 1),
    KEYBOARD_DATA(tab = TopLevelDestination.KEYBOARD, depth = 1),
    SHORTCUTS(tab = TopLevelDestination.SHORTCUTS, depth = 0),
    THEME_GALLERY(tab = TopLevelDestination.APPEARANCE, depth = 0),
    CREATE_CUSTOM_THEME(tab = TopLevelDestination.APPEARANCE, depth = 1),
    NEWS(tab = TopLevelDestination.NEWS, depth = 0),
    ME(tab = TopLevelDestination.ME, depth = 0),
    DATA(tab = TopLevelDestination.ME, depth = 1),
    THIRD_PARTY_LICENSES(tab = TopLevelDestination.ME, depth = 1),
    ;

    companion object {
        /** The screen a tab shows before anything has been pushed onto it. */
        fun rootOf(tab: TopLevelDestination): AppDestination =
            entries.first { destination -> destination.tab == tab && destination.depth == 0 }
    }
}
