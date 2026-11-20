package app.funput.funput.keyboard.ui.clipboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.ui.panel.KeyboardPanelComposeView
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeGradientDirection
import java.time.Instant

internal class ClipboardPanelView(context: Context) : KeyboardPanelComposeView(context) {
    var onSelect: (KeyboardClipboardEntry) -> Unit = {}
    var onTogglePin: (KeyboardClipboardEntry) -> Unit = {}
    var onRemove: (KeyboardClipboardEntry) -> Unit = {}
    var onClearAll: () -> Unit = {}
    var onDelete: () -> Unit = {}
    var onReturn: () -> Unit = {}
    var hapticsEnabled: Boolean
        get() = isHapticFeedbackEnabled
        set(value) { isHapticFeedbackEnabled = value }
    var soundsEnabled: Boolean
        get() = isSoundEffectsEnabled
        set(value) { isSoundEffectsEnabled = value }
    private var entries by mutableStateOf<List<KeyboardClipboardEntry>>(emptyList())
    private var loading by mutableStateOf(false)
    private var now by mutableStateOf(Instant.now())
    private var resetGeneration by mutableIntStateOf(0)

    init { setContent { Content() } }

    fun submit(value: List<KeyboardClipboardEntry>, isLoading: Boolean) {
        entries = value
        loading = isLoading
        now = Instant.now()
    }

    fun updateTheme(theme: KeyboardTheme) {
        palette = KeyboardPanelPalette.from(theme)
        background = GradientDrawable(
            theme.backgroundGradientDirection.androidOrientation,
            intArrayOf(theme.backgroundStartColor, theme.backgroundEndColor),
        )
    }

    fun reset() { resetGeneration += 1 }

    @Composable
    private fun Content() {
        val colors = palette ?: return
        ClipboardPanelContent(
            entries, loading, now, colors, resetGeneration,
            { feedback(KeyboardHapticType.KEY_PRESS); onSelect(it) },
            { feedback(KeyboardHapticType.CONTROL); onTogglePin(it) },
            { feedback(KeyboardHapticType.CONTROL); onRemove(it) },
            { feedback(KeyboardHapticType.CONTROL); onClearAll() },
            { feedback(KeyboardHapticType.DELETE); onDelete() },
            { feedback(KeyboardHapticType.CONTROL); onReturn() },
        )
    }

    private fun feedback(type: KeyboardHapticType) {
        KeyboardHaptics.perform(this, type)
        KeyboardSounds.perform(this, type)
    }
}

private val KeyboardThemeGradientDirection.androidOrientation
    get() = when (this) {
        KeyboardThemeGradientDirection.HORIZONTAL -> GradientDrawable.Orientation.LEFT_RIGHT
        KeyboardThemeGradientDirection.VERTICAL -> GradientDrawable.Orientation.TOP_BOTTOM
        KeyboardThemeGradientDirection.DIAGONAL_DOWN -> GradientDrawable.Orientation.TL_BR
        KeyboardThemeGradientDirection.DIAGONAL_UP -> GradientDrawable.Orientation.TR_BL
    }
