package app.funput.funput.keyboard.ui.clipboard.row

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

/** The pin toggle at the end of a history row: a filled pin when the entry is pinned. */
@Composable
internal fun ClipboardPinAction(
    pinned: Boolean,
    label: String,
    palette: KeyboardPanelPalette,
    onClick: () -> Unit,
) {
    val ink = if (pinned) palette.accent else palette.secondaryLabel
    val color = Color(palette.readableOn(palette.cardSurface, ink))
    Box(
        // A fixed size: a card is as tall as its text, so there is no height to fill.
        Modifier.size(44.dp)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 1.6.dp.toPx()
            drawRoundRect(
                color,
                topLeft = Offset(size.width * 0.22f, 0f),
                size = Size(size.width * 0.56f, size.height * 0.5f),
                style = if (pinned) Fill else Stroke(stroke),
            )
            drawLine(color, center.copy(y = size.height * 0.48f), center.copy(y = size.height), stroke)
        }
    }
}
