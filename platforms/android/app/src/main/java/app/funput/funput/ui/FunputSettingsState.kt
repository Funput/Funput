package app.funput.funput.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.AppearanceSettings
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSelection
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.PersonalSuggestionPreferences
import app.funput.funput.ime.settings.PersonalSuggestionSettings
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.SmartCompositionSettings
import app.funput.funput.ime.settings.ToneStyleSettings
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.theme.KeyboardThemeId

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
    val feedbackStore: KeyboardFeedbackSettings,
    val smartCompositionStore: SmartCompositionSettings,
    val personalSuggestionStore: PersonalSuggestionSettings,
    val inputMethod: KeyboardInputMethod,
    val toneStyle: ToneStyle,
    val keySizeProfile: KeyboardSizingProfile,
    val themeSelection: KeyboardThemeSelection,
    val appearanceMode: AppearanceMode,
    val feedback: KeyboardFeedbackPreferences,
    val smartComposition: SmartCompositionPreferences,
    val personalSuggestions: PersonalSuggestionPreferences,
)

@Composable
internal fun rememberFunputSettings(): FunputSettingsState {
    val context = LocalContext.current
    val input = remember(context) { InputMethodSettings(context) }
    val sizing = remember(context) { KeyboardSizingSettings(context) }
    val keyboardTheme = remember(context) { KeyboardThemeSettings(context) }
    val toneStyleStore = remember(context) { ToneStyleSettings(context) }
    val appearance = remember(context) { AppearanceSettings(context) }
    val feedbackStore = remember(context) { KeyboardFeedbackSettings(context) }
    val smartCompositionStore = remember(context) { SmartCompositionSettings(context) }
    val personalSuggestionStore = remember(context) { PersonalSuggestionSettings(context) }

    val inputMethod by input.inputMethod.collectAsState(InputMethodSettings.DefaultInputMethod)
    val toneStyle by toneStyleStore.toneStyle.collectAsState(ToneStyleSettings.DefaultToneStyle)
    val keySizeProfile by sizing.profile.collectAsState(KeyboardSizingSettings.DefaultProfile)
    val themeSelection by keyboardTheme.selection
        .collectAsState(KeyboardThemeSettings.DefaultSelection)
    val appearanceMode by appearance.mode.collectAsState(AppearanceSettings.DefaultMode)
    val feedback by feedbackStore.preferences.collectAsState(KeyboardFeedbackPreferences.Default)
    val smartComposition by smartCompositionStore.preferences
        .collectAsState(SmartCompositionPreferences.Default)
    val personalSuggestions by personalSuggestionStore.preferences
        .collectAsState(PersonalSuggestionPreferences.Default)

    return FunputSettingsState(
        input = input,
        sizing = sizing,
        keyboardTheme = keyboardTheme,
        toneStyleStore = toneStyleStore,
        appearance = appearance,
        feedbackStore = feedbackStore,
        smartCompositionStore = smartCompositionStore,
        personalSuggestionStore = personalSuggestionStore,
        inputMethod = inputMethod,
        toneStyle = toneStyle,
        keySizeProfile = keySizeProfile,
        themeSelection = themeSelection,
        appearanceMode = appearanceMode,
        feedback = feedback,
        smartComposition = smartComposition,
        personalSuggestions = personalSuggestions,
    )
}
