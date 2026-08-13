package app.funput.funput.keyboard.ui.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val destructive = palette.readable(DestructiveRed, 3.0)
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

@Composable
private fun Action(
    glyph: String?,
    description: String,
    color: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.width(52.dp).fillMaxHeight()
            .semantics { contentDescription = description; if (!enabled) disabled() }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph == null) androidx.compose.foundation.Canvas(Modifier.size(19.dp)) {
            val tint = Color(color).copy(alpha = if (enabled) 1f else 0.35f)
            drawRect(tint, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.3f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.58f), style = Stroke(1.6.dp.toPx()))
            drawLine(tint, androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.22f),
                androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.22f), 1.6.dp.toPx())
        } else BasicText(glyph, style = TextStyle(
                color = Color(color).copy(alpha = if (enabled) 1f else 0.35f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ))
    }
}

@Composable
internal fun ClipboardPreviewOverlay(
    entry: KeyboardClipboardEntry,
    palette: KeyboardPanelPalette,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRemove: () -> Unit,
) {
    val surface = palette.searchSurface or Opaque
    val label = palette.readableOn(surface, palette.label)
    val description = stringResource(R.string.clipboard_preview)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f))
            .semantics { contentDescription = description }
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            Modifier.padding(20.dp).background(Color(surface)).padding(16.dp)
                .clickable {},
        ) {
            BasicText(
                entry.text,
                Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState()),
                TextStyle(Color(label), 14.sp),
            )
            Row(Modifier.padding(top = 12.dp)) {
                PreviewAction(
                    stringResource(if (entry.isPinned) R.string.clipboard_unpin else R.string.clipboard_pin),
                    label,
                    onTogglePin,
                )
                PreviewAction(
                    stringResource(R.string.clipboard_delete),
                    palette.readableOn(surface, DestructiveRed), onRemove,
                )
            }
        }
    }
}

@Composable
private fun PreviewAction(label: String, color: Int, onClick: () -> Unit) {
    BasicText(
        label,
        Modifier.padding(end = 20.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
        TextStyle(Color(color), 14.sp, fontWeight = FontWeight.Medium),
    )
}

private const val DestructiveRed = 0xFFD32F2F.toInt()
private const val Opaque = -0x1000000
