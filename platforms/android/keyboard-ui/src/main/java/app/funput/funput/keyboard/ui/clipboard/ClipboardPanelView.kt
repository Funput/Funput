package app.funput.funput.keyboard.ui.clipboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.ui.panel.KeyboardPanelComposeView
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
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
    private var palette by mutableStateOf<KeyboardPanelPalette?>(null)
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

    @androidx.compose.runtime.Composable
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

private val app.funput.funput.theme.KeyboardThemeGradientDirection.androidOrientation
    get() = when (this) {
        app.funput.funput.theme.KeyboardThemeGradientDirection.HORIZONTAL -> GradientDrawable.Orientation.LEFT_RIGHT
        app.funput.funput.theme.KeyboardThemeGradientDirection.VERTICAL -> GradientDrawable.Orientation.TOP_BOTTOM
        app.funput.funput.theme.KeyboardThemeGradientDirection.DIAGONAL_DOWN -> GradientDrawable.Orientation.TL_BR
        app.funput.funput.theme.KeyboardThemeGradientDirection.DIAGONAL_UP -> GradientDrawable.Orientation.TR_BL
    }

@androidx.compose.runtime.Composable
internal fun ClipboardPinAction(
    pinned: Boolean, label: String, palette: KeyboardPanelPalette, onClick: () -> Unit,
) {
    val color = palette.readable(if (pinned) palette.accent else palette.secondaryLabel)
    Box(
        Modifier.width(44.dp).fillMaxHeight().semantics { contentDescription = label }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
            val style = if (pinned) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.6.dp.toPx())
            drawRoundRect(Color(color), topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.22f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.5f), style = style)
            drawLine(Color(color), center.copy(y = size.height * 0.48f), center.copy(y = size.height), 1.6.dp.toPx())
        }
    }
}
