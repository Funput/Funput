package app.funput.funput.ui
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.customKeyboardThemeStore
import app.funput.funput.ui.about.licenses.LicensesRoute
import app.funput.funput.ui.appearance.AppearanceRoute
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.me.MeRoute
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.AppNavDisplay
import app.funput.funput.ui.navigation.AppTabBar
import app.funput.funput.ui.navigation.TopLevelDestination
import app.funput.funput.ui.navigation.rememberAppNavigator
import app.funput.funput.ui.news.NewsRoute
import app.funput.funput.ui.news.rememberNewsModel
import app.funput.funput.ui.settings.dictionary.PersonalDictionaryRoute
import app.funput.funput.ui.settings.enterkey.EnterKeyRoute
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.route.KeyboardHubRoute
import app.funput.funput.ui.settings.route.SettingsPageRoute
import app.funput.funput.ui.settings.speech.SpeechSetupRoute
import app.funput.funput.ui.shortcuts.ShortcutsRoute
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
    val news = rememberNewsModel()
    val customThemeStore = remember(context) { context.customKeyboardThemeStore() }
    val themeRepository = remember(customThemeStore) { installedThemeRepository(customThemeStore) }
    val customThemeServices =
        rememberCustomThemeServices(themeRepository, customThemeStore, settings.keyboardTheme)
    var themeCatalogRevision by remember { mutableIntStateOf(0) }
    // One read of the theme sources per catalog change. Asking the repository directly would hit
    // disk again on every recomposition, since it deliberately keeps no cache of its own.
    val themeCatalog = remember(themeRepository, themeCatalogRevision) { themeRepository.snapshot() }
    // Saveable, so turning the phone or the process being reclaimed behind the photo picker does
    // not quietly turn "edit this theme" into "make a new one" and save a duplicate.
    var editingThemeValue by rememberSaveable { mutableStateOf<String?>(null) }
    val editingThemeId = KeyboardThemeId.parseOrNull(editingThemeValue)
    // Which slot the gallery assigns to. Pinned to SINGLE unless the user opted into following
    // the system, so nothing about the flow changes for someone who never turns it on.
    var slotChoice by rememberSaveable { mutableStateOf(KeyboardThemeSlot.LIGHT) }
    val activeSlot = if (settings.themeSelection.followsAppearance) {
        slotChoice
    } else {
        KeyboardThemeSlot.SINGLE
    }
    FunputUiTheme(isDark = darkTheme) {
        SyncSystemBarAppearance(darkTheme = darkTheme)
        // The keyboard follows the system appearance rather than the app's own light/dark
        // preference, so the preview has to resolve against the system to show what the user
        // will actually type on.
        val keyboardTheme = themeCatalog.resolve(settings.themeSelection.resolve(isSystemInDarkTheme()))
        AppNavDisplay(navigator) { destination ->
            when (destination) {
                AppDestination.KEYBOARD -> KeyboardHubRoute(
                    settings = settings,
                    keyboardTheme = keyboardTheme,
                    onOpenAppearance = { navigator.selectTab(TopLevelDestination.APPEARANCE) },
                    onOpenPage = { page -> navigator.navigate(page.destinationIn(TopLevelDestination.KEYBOARD)) },
                    tabBar = { AppTabBar(navigator) },
                )
                AppDestination.TYPING, AppDestination.LAYOUT, AppDestination.SMART_INPUT, AppDestination.FEEDBACK,
                AppDestination.CLIPBOARD, AppDestination.HARDWARE_KEYBOARD, AppDestination.DATA,
                AppDestination.KEYBOARD_DATA -> SettingsPageRoute(
                    page = requireNotNull(SettingsPage.of(destination)),
                    settings = settings,
                    keyboardTheme = keyboardTheme,
                    onBack = { navigator.navigateBack() },
                    onOpenDictionary = { navigator.navigate(AppDestination.PERSONAL_DICTIONARY) },
                )
                AppDestination.PERSONAL_DICTIONARY -> PersonalDictionaryRoute(settings) { navigator.navigateBack() }
                AppDestination.SPEECH -> SpeechSetupRoute(onBack = { navigator.navigateBack() })
                AppDestination.ENTER_KEY -> EnterKeyRoute(onBack = { navigator.navigateBack() })
                AppDestination.SHORTCUTS -> ShortcutsRoute(tabBar = { AppTabBar(navigator) })
                AppDestination.THEME_GALLERY -> {
                    AppearanceRoute(
                        settings = settings,
                        catalog = themeCatalog,
                        activeSlot = activeSlot,
                        onSlotSelected = { slot -> slotChoice = slot },
                        onCreateTheme = {
                            editingThemeValue = null
                            navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
                        },
                        onEditTheme = { themeId ->
                            editingThemeValue = themeId.value
                            navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)
                        },
                        onDeleteTheme = { themeId ->
                            scope.launch {
                                customThemeServices.deleteHandler.delete(themeId, settings.themeSelection)
                                themeCatalogRevision += 1
                            }
                        },
                        tabBar = { AppTabBar(navigator) },
                    )
                }
                AppDestination.NEWS -> NewsRoute(news, tabBar = { AppTabBar(navigator) })
                AppDestination.ME -> MeRoute(
                    onOpenData = { navigator.navigate(AppDestination.DATA) },
                    onOpenLicenses = { navigator.navigate(AppDestination.THIRD_PARTY_LICENSES) },
                    tabBar = { AppTabBar(navigator) },
                )
                AppDestination.THIRD_PARTY_LICENSES -> LicensesRoute { navigator.navigateBack() }
                AppDestination.CREATE_CUSTOM_THEME -> {
                    CustomThemeStudioRoute(
                        editingThemeId = editingThemeId,
                        themeRepository = themeRepository,
                        saveHandler = customThemeServices.saveHandler,
                        activeSlot = activeSlot,
                        // The id is left as it is: the studio is still drawn while it animates
                        // out, and clearing the id there would rebuild it as a blank new theme.
                        // Opening the studio again always sets the id first.
                        onDone = {
                            themeCatalogRevision += 1
                            navigator.navigateBack()
                        },
                    )
                }
            }
        }
    }
}
