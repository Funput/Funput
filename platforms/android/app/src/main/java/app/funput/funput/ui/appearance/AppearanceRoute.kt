package app.funput.funput.ui.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeCatalog
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.ui.FunputSettingsState
import app.funput.funput.ui.theme.localizedName
import kotlinx.coroutines.launch

/** Binds the appearance screen to the settings stores and the installed theme catalog. */
@Composable
internal fun AppearanceRoute(
    settings: FunputSettingsState,
    catalog: KeyboardThemeCatalog,
    activeSlot: KeyboardThemeSlot,
    onSlotSelected: (KeyboardThemeSlot) -> Unit,
    onCreateTheme: () -> Unit,
    onEditTheme: (KeyboardThemeId) -> Unit,
    onDeleteTheme: (KeyboardThemeId) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val themes = catalog.themes
    val systemThemes = remember(themes) {
        themes.filter { descriptor -> descriptor.origin == KeyboardThemeOrigin.BUILT_IN }
    }
    val userThemes = remember(themes) {
        themes.filter { descriptor -> descriptor.origin != KeyboardThemeOrigin.BUILT_IN }
    }
    val selection = settings.themeSelection
    AppearanceScreen(
        AppearanceScreenState(
            appearanceMode = settings.appearanceMode,
            dynamicColorEnabled = settings.dynamicColor,
            followsAppearance = selection.followsAppearance,
            activeSlot = activeSlot,
            lightThemeName = catalog.resolve(selection.themeId(KeyboardThemeSlot.LIGHT)).localizedName(),
            darkThemeName = catalog.resolve(selection.themeId(KeyboardThemeSlot.DARK)).localizedName(),
            systemThemes = systemThemes,
            userThemes = userThemes,
            selectedThemeId = selection.themeId(activeSlot),
            onAppearanceSelected = { mode -> scope.launch { settings.appearance.setMode(mode) } },
            onDynamicColorChanged = { enabled ->
                scope.launch { settings.dynamicColorStore.setEnabled(enabled) }
            },
            onFollowsAppearanceChange = { follows ->
                scope.launch { settings.keyboardTheme.setFollowsAppearance(follows) }
            },
            onSlotSelected = onSlotSelected,
            onThemeSelected = { themeId ->
                scope.launch { settings.keyboardTheme.setTheme(themeId, activeSlot) }
            },
            onCreateTheme = onCreateTheme,
            onEditTheme = onEditTheme,
            onDeleteTheme = onDeleteTheme,
        ),
    )
}
