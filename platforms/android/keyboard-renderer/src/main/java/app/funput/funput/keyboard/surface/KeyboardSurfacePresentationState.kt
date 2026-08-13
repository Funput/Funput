package app.funput.funput.keyboard.surface

import app.funput.funput.keyboard.SuggestionNormalizer
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.LocalKeyboardThemeCatalog

/** Owns render-facing state and emits only meaningful changes. */
internal class KeyboardSurfacePresentationState(
    private val onThemeChanged: (KeyboardTheme) -> Unit,
    private val onSizingChanged: (KeyboardSizingProfile) -> Unit,
    private val onBackgroundImageChanged: (KeyboardThemeBackgroundImage?) -> Unit,
    private val onSuggestionsChanged: () -> Unit,
    private val onClipboardHintChanged: () -> Unit = {},
    private val onEnterActionChanged: () -> Unit,
) {
    var keyboardTheme = LocalKeyboardThemeCatalog.defaultTheme.theme
        set(value) {
            if (field == value) return
            field = value
            onThemeChanged(value)
        }
    var keyboardThemeBackgroundImage: KeyboardThemeBackgroundImage? = null
        set(value) {
            if (field == value) return
            field = value
            onBackgroundImageChanged(value)
        }
    var sizingProfile = KeyboardSizingProfile.Default
        set(value) {
            if (field == value) return
            field = value
            onSizingChanged(value)
        }
    var suggestions: List<String> = emptyList()
        set(value) {
            val normalized = SuggestionNormalizer.normalize(value)
            if (field == normalized) return
            field = normalized
            onSuggestionsChanged()
        }
    var clipboardHint: KeyboardClipboardHint? = null
        set(value) {
            if (field == value) return
            field = value
            onClipboardHintChanged()
        }
    var enterAction: KeyboardEnterAction = KeyboardEnterAction.Standard.NEW_LINE
        set(value) {
            if (field == value) return
            field = value
            onEnterActionChanged()
        }
}
