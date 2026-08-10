package app.funput.funput.ui.appearance

import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin

/** A screen state the appearance tests can vary one field of without restating the other sixteen. */
internal fun testAppearanceState(
    extraThemes: List<KeyboardThemeDescriptor> = emptyList(),
    followsAppearance: Boolean = false,
    activeSlot: KeyboardThemeSlot = KeyboardThemeSlot.SINGLE,
    selectedThemeId: KeyboardThemeId = KeyboardThemeId.Dark,
    onThemeSelected: (KeyboardThemeId) -> Unit = {},
    onFollowsAppearanceChange: (Boolean) -> Unit = {},
    onSlotSelected: (KeyboardThemeSlot) -> Unit = {},
    onCreateTheme: () -> Unit = {},
    onEditTheme: (KeyboardThemeId) -> Unit = {},
    onDeleteTheme: (KeyboardThemeId) -> Unit = {},
): AppearanceScreenState {
    val themes = InstalledThemeRepository.builtIn().themes + extraThemes
    return AppearanceScreenState(
        appearanceMode = AppearanceMode.SYSTEM,
        dynamicColorEnabled = false,
        followsAppearance = followsAppearance,
        activeSlot = activeSlot,
        lightThemeName = "Paper",
        darkThemeName = "Ink",
        systemThemes = themes.filter { it.origin == KeyboardThemeOrigin.BUILT_IN },
        userThemes = themes.filter { it.origin != KeyboardThemeOrigin.BUILT_IN },
        selectedThemeId = selectedThemeId,
        onAppearanceSelected = {},
        onDynamicColorChanged = {},
        onFollowsAppearanceChange = onFollowsAppearanceChange,
        onSlotSelected = onSlotSelected,
        onThemeSelected = onThemeSelected,
        onCreateTheme = onCreateTheme,
        onEditTheme = onEditTheme,
        onDeleteTheme = onDeleteTheme,
    )
}
