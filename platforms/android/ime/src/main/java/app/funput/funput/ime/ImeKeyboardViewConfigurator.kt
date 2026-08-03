package app.funput.funput.ime

import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.keyboard.KeyboardFeatures
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage

/**
 * Pushes the current settings and editor state onto the keyboard view.
 *
 * The theme is resolved here rather than by the service so the appearance the keyboard is being
 * drawn for is always passed in explicitly, instead of being read from ambient state at whichever
 * moment the view happens to be refreshed.
 */
internal fun FunputKeyboardView.applyImeState(
    settings: ImeSettingsController,
    policy: EditorInfoPolicy,
    currentLanguage: KeyboardLanguage,
    themeRepository: InstalledThemeRepository,
    darkAppearance: Boolean,
) {
    val descriptor = themeRepository.resolveForAppearance(settings.themeSelection, darkAppearance)
    configureForEditor(
        inputMethod = settings.inputMethod,
        policy = policy,
        currentLanguage = currentLanguage,
        feedback = settings.feedback,
        sizingProfile = settings.sizingProfile,
        keyboardTheme = descriptor.theme,
        keyboardThemeBackgroundImage = descriptor.backgroundImage,
        showsNumberRow = settings.showsNumberRow,
    )
}

internal fun FunputKeyboardView.configureForEditor(
    inputMethod: KeyboardInputMethod,
    policy: EditorInfoPolicy,
    currentLanguage: KeyboardLanguage,
    feedback: KeyboardFeedbackPreferences,
    sizingProfile: KeyboardSizingProfile,
    keyboardTheme: KeyboardTheme,
    keyboardThemeBackgroundImage: KeyboardThemeBackgroundImage?,
    showsNumberRow: Boolean,
) {
    showLettersPanel()
    this.inputMethod = inputMethod
    this.showsNumberRow = showsNumberRow
    this.sizingProfile = sizingProfile
    this.keyboardTheme = keyboardTheme
    this.keyboardThemeBackgroundImage = keyboardThemeBackgroundImage
    systemInputMethodSwitcherVisible = false
    editorMode = policy.editorMode
    suggestionBarEnabled = KeyboardFeatures.SuggestionsEnabled && policy.showsSuggestionBar
    language = if (policy.editorMode.supportsVietnameseComposition) {
        currentLanguage
    } else {
        KeyboardLanguage.ENGLISH
    }
    enterAction = policy.editorAction.presentation
    hapticsEnabled = feedback.hapticsEnabled
    soundsEnabled = feedback.soundsEnabled
}
