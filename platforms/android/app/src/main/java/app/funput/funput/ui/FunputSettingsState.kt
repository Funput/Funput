package app.funput.funput.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.AppearanceSettings
import app.funput.funput.ime.settings.DynamicColorSettings
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSelection
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.NumberRowSettings
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.SmartCompositionSettings
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ime.settings.ToneStyleSettings
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod

/**
 * Every persisted setting the app screens read, plus the stores they write back through.
 *
 * Keeping the DataStore wiring here leaves [FunputApp] to do navigation and nothing else, which
 * matters because that file sits against the per-file line limit.
 */
@Stable
internal class FunputSettingsState(
    val input: InputMethodSettings,
    val sizing: KeyboardSizingSettings,
    val keyboardTheme: KeyboardThemeSettings,
    val toneStyleStore: ToneStyleSettings,
    val appearance: AppearanceSettings,
    val dynamicColorStore: DynamicColorSettings,
    val feedbackStore: KeyboardFeedbackSettings,
    val numberRowStore: NumberRowSettings,
    val smartCompositionStore: SmartCompositionSettings,
    val personalSuggestionStore: PersonalSuggestionSettings,
    val inputMethod: KeyboardInputMethod,
    val toneStyle: ToneStyle,
    val keySizeProfile: KeyboardSizingProfile,
    val themeSelection: KeyboardThemeSelection,
    val appearanceMode: AppearanceMode,
    val dynamicColor: Boolean,
    val feedback: KeyboardFeedbackPreferences,
    val showsNumberRow: Boolean,
    val smartComposition: SmartCompositionPreferences,
    val personalSuggestions: PersonalSuggestionPreferences,
)

@Composable
internal fun rememberFunputSettings(): FunputSettingsState {
    val context = LocalContext.current
    val stores = remember(context) { FunputSettingsStores(context) }
    val inputMethod by stores.input.inputMethod.collectAsState(InputMethodSettings.DefaultInputMethod)
    val toneStyle by stores.toneStyleStore.toneStyle.collectAsState(ToneStyleSettings.DefaultToneStyle)
    val keySizeProfile by stores.sizing.profile.collectAsState(KeyboardSizingSettings.DefaultProfile)
    val themeSelection by stores.keyboardTheme.selection
        .collectAsState(KeyboardThemeSettings.DefaultSelection)
    val appearanceMode by stores.appearance.mode.collectAsState(AppearanceSettings.DefaultMode)
    val dynamicColor by stores.dynamicColorStore.enabled
        .collectAsState(DynamicColorSettings.DefaultEnabled)
    val feedback by stores.feedbackStore.preferences.collectAsState(KeyboardFeedbackPreferences.Default)
    val showsNumberRow by stores.numberRowStore.showsNumberRow
        .collectAsState(NumberRowSettings.DefaultShowsNumberRow)
    val smartComposition by stores.smartCompositionStore.preferences
        .collectAsState(SmartCompositionPreferences.Default)
    val personalSuggestions by stores.personalSuggestionStore.preferences
        .collectAsState(PersonalSuggestionPreferences.Default)

    return FunputSettingsState(
        input = stores.input,
        sizing = stores.sizing,
        keyboardTheme = stores.keyboardTheme,
        toneStyleStore = stores.toneStyleStore,
        appearance = stores.appearance,
        dynamicColorStore = stores.dynamicColorStore,
        feedbackStore = stores.feedbackStore,
        numberRowStore = stores.numberRowStore,
        smartCompositionStore = stores.smartCompositionStore,
        personalSuggestionStore = stores.personalSuggestionStore,
        inputMethod = inputMethod,
        toneStyle = toneStyle,
        keySizeProfile = keySizeProfile,
        themeSelection = themeSelection,
        appearanceMode = appearanceMode,
        dynamicColor = dynamicColor,
        feedback = feedback,
        showsNumberRow = showsNumberRow,
        smartComposition = smartComposition,
        personalSuggestions = personalSuggestions,
    )
}
