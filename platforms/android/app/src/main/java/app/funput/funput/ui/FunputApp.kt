package app.funput.funput.ui
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.customKeyboardThemeStore
import app.funput.funput.ui.keyboard.openKeyboardSettings
import app.funput.funput.ui.keyboard.openWebsite
import app.funput.funput.ui.keyboard.showKeyboardPicker
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.rememberAppNavigator
import app.funput.funput.ui.settings.SettingsScreen
import app.funput.funput.ui.settings.setup.rememberKeyboardSetupStatus
import app.funput.funput.ui.theme.FunputTheme
import app.funput.funput.ui.theme.custom.CustomThemeStudioRoute
import app.funput.funput.ui.theme.custom.rememberCustomThemeServices
import app.funput.funput.ui.theme.gallery.ThemeGalleryScreen
import app.funput.funput.ui.theme.localizedName
import app.funput.funput.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch
@Composable
fun FunputApp() {
    val context = LocalContext.current
    val settings = rememberFunputSettings()
    val versionName = remember(context) { AppVersionProvider.versionName(context) }
    val keyboardSetupStatus = rememberKeyboardSetupStatus()
    val scope = rememberCoroutineScope()
    val darkTheme = settings.appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    val websiteUrl = stringResource(R.string.settings_website_url)
    val navigator = rememberAppNavigator()
    val customThemeStore = remember(context) { context.customKeyboardThemeStore() }
    val themeRepository = remember(customThemeStore) { installedThemeRepository(customThemeStore) }
    val customThemeServices =
        rememberCustomThemeServices(themeRepository, customThemeStore, settings.keyboardTheme)
    var themeCatalogRevision by remember { mutableStateOf(0) }
    val installedThemes = remember(themeRepository, themeCatalogRevision) { themeRepository.themes }
    val keyboardThemeLabel = themeRepository.resolve(settings.keyboardThemeId).localizedName()
    var editingThemeId by remember { mutableStateOf<KeyboardThemeId?>(null) }
    BackHandler(enabled = navigator.canNavigateBack) { navigator.navigateBack() }
    FunputTheme(appearanceMode = settings.appearanceMode) {
        SyncSystemBarAppearance(darkTheme = darkTheme)
        Surface(modifier = Modifier.fillMaxSize()) {
            when (navigator.currentDestination) {
                AppDestination.SETTINGS -> SettingsScreen(
                    keyboardSetupStatus = keyboardSetupStatus,
                    inputMethod = settings.inputMethod,
                    toneStyle = settings.toneStyle,
                    keySizeProfile = settings.keySizeProfile,
                    keyboardThemeId = settings.keyboardThemeId,
                    keyboardThemeLabel = keyboardThemeLabel,
                    appearanceMode = settings.appearanceMode,
                    hapticsEnabled = settings.feedback.hapticsEnabled,
                    soundsEnabled = settings.feedback.soundsEnabled,
                    smartRestoreEnabled = settings.smartComposition.smartRestoreEnabled,
                    spellCheckEnabled = settings.smartComposition.spellCheckEnabled,
                    personalSuggestionsEnabled = settings.personalSuggestions.enabled,
                    versionName = versionName,
                    onInputMethodSelected = { method -> scope.launch { settings.input.setInputMethod(method) } },
                    onToneStyleSelected = { style -> scope.launch { settings.toneStyleStore.setToneStyle(style) } },
                    onKeySizeSelected = { profile -> scope.launch { settings.sizing.setProfile(profile) } },
                    onAppearanceSelected = { mode -> scope.launch { settings.appearance.setMode(mode) } },
                    onHapticsChanged = { enabled ->
                        scope.launch { settings.feedbackStore.setHapticsEnabled(enabled) }
                    },
                    onSoundsChanged = { enabled ->
                        scope.launch { settings.feedbackStore.setSoundsEnabled(enabled) }
                    },
                    onSmartRestoreChanged = { enabled ->
                        scope.launch { settings.smartCompositionStore.setSmartRestoreEnabled(enabled) }
                    },
                    onSpellCheckChanged = { enabled ->
                        scope.launch { settings.smartCompositionStore.setSpellCheckEnabled(enabled) }
                    },
                    onPersonalSuggestionsChanged = { enabled ->
                        scope.launch { settings.personalSuggestionStore.setEnabled(enabled) }
                    },
                    onResetPersonalSuggestions = {
                        scope.launch { settings.personalSuggestionStore.requestReset() }
                    },
                    onEnableKeyboard = context::openKeyboardSettings,
                    onSelectKeyboard = context::showKeyboardPicker,
                    onOpenThemeGallery = { navigator.navigate(AppDestination.THEME_GALLERY) },
                    onOpenWebsite = { context.openWebsite(websiteUrl) },
                )
                AppDestination.THEME_GALLERY -> ThemeGalleryScreen(
                    themes = installedThemes,
                    selectedThemeId = settings.keyboardThemeId,
                    onThemeSelected = { themeId ->
                        scope.launch { settings.keyboardTheme.setTheme(themeId) }
                    },
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
                            customThemeServices.deleteHandler.delete(themeId, settings.keyboardThemeId)
                            themeCatalogRevision += 1
                        }
                    },
                    onBack = navigator::navigateBack,
                )
                AppDestination.CREATE_CUSTOM_THEME -> CustomThemeStudioRoute(
                    editingThemeId = editingThemeId,
                    themeRepository = themeRepository,
                    saveHandler = customThemeServices.saveHandler,
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
