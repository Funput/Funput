package app.funput.funput.ui
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceSettings
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.SmartCompositionSettings
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.ToneStyleSettings
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
    val inputSettings = remember(context) { InputMethodSettings(context) }
    val sizingSettings = remember(context) { KeyboardSizingSettings(context) }
    val keyboardThemeSettings = remember(context) { KeyboardThemeSettings(context) }
    val toneStyleSettings = remember(context) { ToneStyleSettings(context) }
    val appearanceSettings = remember(context) { AppearanceSettings(context) }
    val feedbackSettings = remember(context) { KeyboardFeedbackSettings(context) }
    val smartCompositionSettings = remember(context) { SmartCompositionSettings(context) }
    val personalSuggestionSettings = remember(context) { PersonalSuggestionSettings(context) }
    val versionName = remember(context) { AppVersionProvider.versionName(context) }
    val keyboardSetupStatus = rememberKeyboardSetupStatus()
    val inputMethod by inputSettings.inputMethod.collectAsState(InputMethodSettings.DefaultInputMethod)
    val toneStyle by toneStyleSettings.toneStyle.collectAsState(ToneStyleSettings.DefaultToneStyle)
    val keySizeProfile by sizingSettings.profile.collectAsState(KeyboardSizingSettings.DefaultProfile)
    val keyboardThemeId by keyboardThemeSettings.themeId.collectAsState(KeyboardThemeSettings.DefaultThemeId)
    val appearanceMode by appearanceSettings.mode.collectAsState(AppearanceSettings.DefaultMode)
    val feedback by feedbackSettings.preferences.collectAsState(KeyboardFeedbackPreferences.Default)
    val smartComposition by smartCompositionSettings.preferences.collectAsState(SmartCompositionPreferences.Default)
    val personalSuggestions by personalSuggestionSettings.preferences.collectAsState(PersonalSuggestionPreferences.Default)
    val scope = rememberCoroutineScope()
    val darkTheme = appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    val websiteUrl = stringResource(R.string.settings_website_url)
    val navigator = rememberAppNavigator()
    val customThemeStore = remember(context) { context.customKeyboardThemeStore() }
    val themeRepository = remember(customThemeStore) { installedThemeRepository(customThemeStore) }
    val customThemeServices = rememberCustomThemeServices(themeRepository, customThemeStore, keyboardThemeSettings)
    var themeCatalogRevision by remember { mutableStateOf(0) }
    val installedThemes = remember(themeRepository, themeCatalogRevision) { themeRepository.themes }
    val keyboardThemeLabel = themeRepository.resolve(keyboardThemeId).localizedName()
    var editingThemeId by remember { mutableStateOf<KeyboardThemeId?>(null) }
    BackHandler(enabled = navigator.canNavigateBack) { navigator.navigateBack() }
    FunputTheme(appearanceMode = appearanceMode) {
        SyncSystemBarAppearance(darkTheme = darkTheme)
        Surface(modifier = Modifier.fillMaxSize()) {
            when (navigator.currentDestination) {
                AppDestination.SETTINGS -> SettingsScreen(
                    keyboardSetupStatus = keyboardSetupStatus,
                    inputMethod = inputMethod,
                    toneStyle = toneStyle,
                    keySizeProfile = keySizeProfile,
                    keyboardThemeId = keyboardThemeId,
                    keyboardThemeLabel = keyboardThemeLabel,
                    appearanceMode = appearanceMode,
                    hapticsEnabled = feedback.hapticsEnabled,
                    soundsEnabled = feedback.soundsEnabled,
                    smartRestoreEnabled = smartComposition.smartRestoreEnabled,
                    spellCheckEnabled = smartComposition.spellCheckEnabled,
                    personalSuggestionsEnabled = personalSuggestions.enabled,
                    versionName = versionName,
                    onInputMethodSelected = { method -> scope.launch { inputSettings.setInputMethod(method) } },
                    onToneStyleSelected = { style -> scope.launch { toneStyleSettings.setToneStyle(style) } },
                    onKeySizeSelected = { profile -> scope.launch { sizingSettings.setProfile(profile) } },
                    onAppearanceSelected = { mode -> scope.launch { appearanceSettings.setMode(mode) } },
                    onHapticsChanged = { enabled -> scope.launch { feedbackSettings.setHapticsEnabled(enabled) } },
                    onSoundsChanged = { enabled -> scope.launch { feedbackSettings.setSoundsEnabled(enabled) } },
                    onSmartRestoreChanged = { enabled ->
                        scope.launch { smartCompositionSettings.setSmartRestoreEnabled(enabled) }
                    },
                    onSpellCheckChanged = { enabled ->
                        scope.launch { smartCompositionSettings.setSpellCheckEnabled(enabled) }
                    },
                    onPersonalSuggestionsChanged = { enabled -> scope.launch {
                        personalSuggestionSettings.setEnabled(enabled) } },
                    onResetPersonalSuggestions = { scope.launch { personalSuggestionSettings.requestReset() } },
                    onEnableKeyboard = context::openKeyboardSettings,
                    onSelectKeyboard = context::showKeyboardPicker,
                    onOpenThemeGallery = { navigator.navigate(AppDestination.THEME_GALLERY) },
                    onOpenWebsite = { context.openWebsite(websiteUrl) },
                )
                AppDestination.THEME_GALLERY -> ThemeGalleryScreen(
                    themes = installedThemes,
                    selectedThemeId = keyboardThemeId,
                    onThemeSelected = { themeId ->
                        scope.launch { keyboardThemeSettings.setTheme(themeId) }
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
                            customThemeServices.deleteHandler.delete(themeId, keyboardThemeId)
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
