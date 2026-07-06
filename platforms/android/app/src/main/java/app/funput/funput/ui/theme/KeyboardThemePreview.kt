package app.funput.funput.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme

/** Stable keyboard state used while presenting a theme outside the IME. */
internal data class KeyboardThemePreviewConfiguration(
    val inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX,
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.LETTERS,
    val editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
    val language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE,
    val shiftState: ShiftState = ShiftState.OFF,
    val sizingProfile: KeyboardSizingProfile = KeyboardSizingProfile.Normal,
    val suggestionBarEnabled: Boolean = true,
    val suggestions: List<String> = emptyList(),
    val enterAction: KeyboardEnterAction = KeyboardEnterAction.Standard.NEW_LINE,
)

/**
 * Renders a keyboard theme with the production Canvas renderer without connecting to an editor.
 *
 * The composable accepts resolved visual tokens rather than a catalog ID. This allows both
 * installed themes and an unsaved custom-theme draft to use the same preview path.
 */
@Composable
internal fun KeyboardThemePreview(
    theme: KeyboardTheme,
    modifier: Modifier = Modifier,
    configuration: KeyboardThemePreviewConfiguration = KeyboardThemePreviewConfiguration(),
) {
    AndroidView(
        factory = { context ->
            KeyboardSurfaceView(context).apply {
                interactionEnabled = false
            }
        },
        modifier = modifier,
        update = { view ->
            view.applyPreview(theme, configuration)
        },
    )
}

private fun KeyboardSurfaceView.applyPreview(
    theme: KeyboardTheme,
    configuration: KeyboardThemePreviewConfiguration,
) {
    keyboardTheme = theme
    inputMethod = configuration.inputMethod
    layoutMode = configuration.layoutMode
    editorMode = configuration.editorMode
    language = configuration.language
    shiftState = configuration.shiftState
    sizingProfile = configuration.sizingProfile
    suggestionBarEnabled = configuration.suggestionBarEnabled
    suggestions = configuration.suggestions
    enterAction = configuration.enterAction
    systemInputMethodSwitcherVisible = false
}
