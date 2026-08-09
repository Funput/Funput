package app.funput.funput.ui.appearance

import androidx.compose.runtime.Immutable
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

/**
 * Everything [AppearanceScreen] draws and every way out of it.
 *
 * The screen took eleven parameters once the app's own colours joined the keyboard's; a holder
 * keeps its signature readable and keeps the sections below from each growing their own long
 * parameter list.
 */
@Immutable
internal class AppearanceScreenState(
    val appearanceMode: AppearanceMode,
    val dynamicColorEnabled: Boolean,
    val followsAppearance: Boolean,
    val activeSlot: KeyboardThemeSlot,
    val lightThemeName: String,
    val darkThemeName: String,
    val systemThemes: List<KeyboardThemeDescriptor>,
    val userThemes: List<KeyboardThemeDescriptor>,
    val selectedThemeId: KeyboardThemeId,
    val onAppearanceSelected: (AppearanceMode) -> Unit,
    val onDynamicColorChanged: (Boolean) -> Unit,
    val onFollowsAppearanceChange: (Boolean) -> Unit,
    val onSlotSelected: (KeyboardThemeSlot) -> Unit,
    val onThemeSelected: (KeyboardThemeId) -> Unit,
    val onCreateTheme: () -> Unit,
    val onEditTheme: (KeyboardThemeId) -> Unit,
    val onDeleteTheme: (KeyboardThemeId) -> Unit,
)
