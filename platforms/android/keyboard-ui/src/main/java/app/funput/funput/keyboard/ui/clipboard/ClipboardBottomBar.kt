package app.funput.funput.keyboard.ui.clipboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

@Composable
internal fun ClipboardBottomBar(
    palette: KeyboardPanelPalette,
    canClear: Boolean,
    onReturn: () -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    val label = palette.readable(palette.label)
    val destructive = palette.readable(ClipboardDestructiveRed, 3.0)
    Row(
        Modifier.fillMaxSize().background(Color(palette.divider).copy(alpha = 0.08f)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Action("ABC", stringResource(R.string.clipboard_return), label, true, onReturn)
        Row {
            Action(null, stringResource(R.string.clipboard_clear_all), destructive, canClear, onClear)
            Action("⌫", stringResource(R.string.clipboard_delete), label, true, onDelete)
        }
    }
}

/** One bar button: [glyph] as text, or the drawn bin icon when there is no glyph for it. */
@Composable
private fun Action(
    glyph: String?,
    description: String,
    color: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = Color(color).copy(alpha = if (enabled) 1f else 0.35f)
    Box(
        Modifier.width(52.dp).fillMaxHeight()
            .semantics { contentDescription = description; if (!enabled) disabled() }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph == null) {
            Canvas(Modifier.size(19.dp)) {
                val stroke = 1.6.dp.toPx()
                drawRect(
                    tint,
                    topLeft = Offset(size.width * 0.25f, size.height * 0.3f),
                    size = Size(size.width * 0.5f, size.height * 0.58f),
                    style = Stroke(stroke),
                )
                drawLine(
                    tint,
                    Offset(size.width * 0.18f, size.height * 0.22f),
                    Offset(size.width * 0.82f, size.height * 0.22f),
                    stroke,
                )
            }
        } else {
            BasicText(glyph, style = TextStyle(tint, 16.sp, FontWeight.Medium, textAlign = TextAlign.Center))
        }
    }
}

internal const val ClipboardDestructiveRed = 0xFFD32F2F.toInt()
