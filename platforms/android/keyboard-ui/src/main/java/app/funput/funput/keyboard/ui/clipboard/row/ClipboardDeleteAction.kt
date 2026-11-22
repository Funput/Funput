package app.funput.funput.keyboard.ui.clipboard.row

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The delete action lying under a history card.
 *
 * The card is translucent, so the action cannot simply sit behind it: only the strip the card
 * has slid away from, [uncovered] pixels wide, is drawn.
 */
@Composable
internal fun ClipboardDeleteAction(
    label: String,
    uncovered: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.drawWithContent {
            clipRect(left = size.width - uncovered()) { this@drawWithContent.drawContent() }
        },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            Modifier.width(ClipboardDeleteWidth).fillMaxHeight().background(DeleteSurface)
                .semantics { contentDescription = label; role = Role.Button }
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(16.dp)) {
                val stroke = 1.5.dp.toPx()
                drawRect(
                    Color.White,
                    topLeft = Offset(size.width * 0.25f, size.height * 0.3f),
                    size = Size(size.width * 0.5f, size.height * 0.58f),
                    style = Stroke(stroke),
                )
                drawLine(
                    Color.White,
                    Offset(size.width * 0.18f, size.height * 0.22f),
                    Offset(size.width * 0.82f, size.height * 0.22f),
                    stroke,
                )
            }
            BasicText(label, style = TextStyle(Color.White, 12.sp))
        }
    }
}

private val DeleteSurface = Color(0xFFB3261E)
