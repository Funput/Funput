package app.funput.funput.ime

import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.keyboard.KeyboardFeatures
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage

internal fun FunputKeyboardView.configureForEditor(
    inputMethod: KeyboardInputMethod,
    policy: EditorInfoPolicy,
    currentLanguage: KeyboardLanguage,
    feedback: KeyboardFeedbackPreferences,
    sizingProfile: KeyboardSizingProfile,
    keyboardTheme: KeyboardTheme,
    keyboardThemeBackgroundImage: KeyboardThemeBackgroundImage?,
) {
    showLettersPanel()
    this.inputMethod = inputMethod
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
