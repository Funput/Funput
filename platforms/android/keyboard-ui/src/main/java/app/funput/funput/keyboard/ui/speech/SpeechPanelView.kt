package app.funput.funput.keyboard.ui.speech

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.ui.speech.visual.SpeechPanelStyle
import app.funput.funput.keyboard.ui.panel.KeyboardPanelComposeView
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeGradientDirection

/** Attach and recomposition only render state; actions require a user gesture. */
internal class SpeechPanelView(context: Context) : KeyboardPanelComposeView(context) {
    var onAction: (SpeechPanelAction) -> Unit = {}
    var state by mutableStateOf(SpeechPanelState())
        private set
    private var palette by mutableStateOf<KeyboardPanelPalette?>(null)

    private var style by mutableStateOf<SpeechPanelStyle?>(null)

    init {
        setContent {
            palette?.let { colors -> style?.let { SpeechPanelContent(state, colors, it, ::dispatch) } }
        }
    }

    fun submit(value: SpeechPanelState) { state = value }

    fun clear() { state = SpeechPanelState() }

    fun updateTheme(theme: KeyboardTheme) {
        palette = KeyboardPanelPalette.from(theme)
        style = SpeechPanelStyle.from(theme)
        val orientation = when (theme.backgroundGradientDirection) {
            KeyboardThemeGradientDirection.HORIZONTAL -> GradientDrawable.Orientation.LEFT_RIGHT
            KeyboardThemeGradientDirection.VERTICAL -> GradientDrawable.Orientation.TOP_BOTTOM
            KeyboardThemeGradientDirection.DIAGONAL_DOWN -> GradientDrawable.Orientation.TL_BR
            KeyboardThemeGradientDirection.DIAGONAL_UP -> GradientDrawable.Orientation.TR_BL
        }
        background = GradientDrawable(orientation, intArrayOf(theme.backgroundStartColor, theme.backgroundEndColor))
    }

    private fun dispatch(action: SpeechPanelAction) {
        KeyboardHaptics.perform(this, KeyboardHapticType.CONTROL)
        KeyboardSounds.perform(this, KeyboardHapticType.CONTROL)
        onAction(action)
    }
}
