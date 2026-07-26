package app.funput.funput.ime.settings

import app.funput.funput.theme.KeyboardThemeId

/**
 * Which keyboard theme applies, given the system's light or dark appearance.
 *
 * Funput themes carry one palette each rather than a light/dark pair, so following the system
 * means choosing two themes rather than one theme that adapts. [singleThemeId] is what applies
 * when the user would rather pin one look regardless of the system.
 */
data class KeyboardThemeSelection(
    val singleThemeId: KeyboardThemeId = KeyboardThemeId.Default,
    val lightThemeId: KeyboardThemeId = KeyboardThemeId.Light,
    val darkThemeId: KeyboardThemeId = KeyboardThemeId.Dark,
    val followsAppearance: Boolean = false,
) {
    fun resolve(darkAppearance: Boolean): KeyboardThemeId = when {
        !followsAppearance -> singleThemeId
        darkAppearance -> darkThemeId
        else -> lightThemeId
    }

    /** Replaces whichever slot the given editing mode is pointing at. */
    fun withTheme(themeId: KeyboardThemeId, slot: KeyboardThemeSlot): KeyboardThemeSelection =
        when (slot) {
            KeyboardThemeSlot.SINGLE -> copy(singleThemeId = themeId)
            KeyboardThemeSlot.LIGHT -> copy(lightThemeId = themeId)
            KeyboardThemeSlot.DARK -> copy(darkThemeId = themeId)
        }

    fun themeId(slot: KeyboardThemeSlot): KeyboardThemeId = when (slot) {
        KeyboardThemeSlot.SINGLE -> singleThemeId
        KeyboardThemeSlot.LIGHT -> lightThemeId
        KeyboardThemeSlot.DARK -> darkThemeId
    }

    /** Every theme this selection can produce, used to repoint a slot when a theme is deleted. */
    fun slotsUsing(themeId: KeyboardThemeId): List<KeyboardThemeSlot> =
        KeyboardThemeSlot.entries.filter { slot -> themeId(slot) == themeId }
}

/** The slot a gallery selection writes to. */
enum class KeyboardThemeSlot {
    SINGLE,
    LIGHT,
    DARK,
}
