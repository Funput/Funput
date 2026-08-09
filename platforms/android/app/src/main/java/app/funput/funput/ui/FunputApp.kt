package app.funput.funput.ui
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.funput.funput.ime.settings.KeyboardThemeSlot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.customKeyboardThemeStore
import app.funput.funput.ui.about.AboutRoute
import app.funput.funput.ui.appearance.AppearanceRoute
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.AppNavDisplay
import app.funput.funput.ui.navigation.AppNavigationSuite
import app.funput.funput.ui.navigation.TopLevelDestination
import app.funput.funput.ui.navigation.rememberAppNavigator
import app.funput.funput.ui.theme.FunputTheme
import app.funput.funput.ui.theme.custom.CustomThemeStudioRoute
import app.funput.funput.ui.theme.custom.rememberCustomThemeServices
import app.funput.funput.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch
@Composable
fun FunputApp() {
    val context = LocalContext.current
    val settings = rememberFunputSettings()
    val scope = rememberCoroutineScope()
    val darkTheme = settings.appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    val navigator = rememberAppNavigator()
    val customThemeStore = remember(context) { context.customKeyboardThemeStore() }
    val themeRepository = remember(customThemeStore) { installedThemeRepository(customThemeStore) }
    val customThemeServices =
        rememberCustomThemeServices(themeRepository, customThemeStore, settings.keyboardTheme)
    var themeCatalogRevision by remember { mutableStateOf(0) }
    // One read of the theme sources per catalog change. Asking the repository directly would hit
    // disk again on every recomposition, since it deliberately keeps no cache of its own.
    val themeCatalog = remember(themeRepository, themeCatalogRevision) { themeRepository.snapshot() }
    var editingThemeId by remember { mutableStateOf<KeyboardThemeId?>(null) }
    // Which slot the gallery assigns to. Pinned to SINGLE unless the user opted into following
    // the system, so nothing about the flow changes for someone who never turns it on.
    var slotChoice by rememberSaveable { mutableStateOf(KeyboardThemeSlot.LIGHT) }
    val activeSlot = if (settings.themeSelection.followsAppearance) {
        slotChoice
    } else {
        KeyboardThemeSlot.SINGLE
    }
    FunputTheme(appearanceMode = settings.appearanceMode, dynamicColor = settings.dynamicColor) {
        SyncSystemBarAppearance(darkTheme = darkTheme)
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigationSuite(navigator) {
            AppNavDisplay(navigator) { destination ->
                when (destination) {
                    AppDestination.SETTINGS -> SettingsRoute(
                        settings = settings,
                        // The keyboard follows the system appearance rather than the app's own
                        // light/dark preference, so the hero has to resolve against the system to
                        // show what the user will actually type on.
                        keyboardTheme = themeCatalog.resolve(
                            settings.themeSelection.resolve(isSystemInDarkTheme()),
                        ),
                        onOpenAppearance = { navigator.selectTab(TopLevelDestination.APPEARANCE) },
                    )
                    AppDestination.THEME_GALLERY -> AppearanceRoute(
                        settings = settings,
                        catalog = themeCatalog,
                        activeSlot = activeSlot,
                        onSlotSelected = { slot -> slotChoice = slot },
                        onCreateTheme = {
                            editingThemeId = null
                            navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
                        },
                        onEditTheme = { themeId ->
                            editingThemeId = themeId
                            navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
                        },
                        onDeleteTheme = { themeId ->
                            scope.launch {
                                customThemeServices.deleteHandler.delete(themeId, settings.themeSelection)
                                themeCatalogRevision += 1
                            }
                        },
                    )
                    AppDestination.ABOUT -> AboutRoute()
                    AppDestination.CREATE_CUSTOM_THEME -> CustomThemeStudioRoute(
                        editingThemeId = editingThemeId,
                        themeRepository = themeRepository,
                        saveHandler = customThemeServices.saveHandler,
                        activeSlot = activeSlot,
                        onDone = {
                            themeCatalogRevision += 1
                            editingThemeId = null
                            navigator.navigateBack()
                        },
                    )
                }
            }
            }
        }
    }
}
