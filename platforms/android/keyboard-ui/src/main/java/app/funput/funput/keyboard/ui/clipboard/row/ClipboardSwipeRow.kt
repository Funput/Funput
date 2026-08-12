package app.funput.funput.keyboard.ui.clipboard.row

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.clipboard.ClipboardPinAction
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import java.time.Instant
import kotlin.math.roundToInt

private val ActionWidth = 88.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ClipboardSwipeRow(
    entry: KeyboardClipboardEntry, now: Instant, palette: KeyboardPanelPalette,
    revealed: Boolean, onReveal: (Boolean) -> Unit, onSelect: () -> Unit,
    onPin: () -> Unit, onRemove: () -> Unit, onPreview: () -> Unit,
) {
    val widthPx = with(LocalDensity.current) { ActionWidth.toPx() }
    var dragging by remember(entry.id) { mutableStateOf(false) }
    var dragOffset by remember(entry.id) { mutableFloatStateOf(0f) }
    val target = if (revealed) -widthPx else 0f
    val animated by animateFloatAsState(target, label = "clipboard swipe")
    val offset = if (dragging) dragOffset else animated
    val preview = ClipboardRowTextContent.preview(entry.text)
    val pinLabel = stringResource(if (entry.isPinned) R.string.clipboard_unpin else R.string.clipboard_pin)
    val deleteLabel = stringResource(R.string.clipboard_delete)
    Box(Modifier.fillMaxWidth().height(44.dp)) {
        DeleteAction(deleteLabel, { onReveal(false); onRemove() }, Modifier.align(Alignment.CenterEnd))
        Row(
            Modifier.fillMaxSize().offset { IntOffset(offset.roundToInt(), 0) }
                .background(Color(palette.backgroundEnd))
                .pointerInput(entry.id, revealed, widthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true; dragOffset = target },
                        onDragCancel = { dragging = false },
                        onDragEnd = {
                            onReveal(shouldRevealClipboardAction(dragOffset, widthPx))
                            dragging = false
                        },
                    ) { change, amount ->
                        change.consume()
                        dragOffset = (dragOffset + amount).coerceIn(-widthPx, 0f)
                    }
                }
                .semantics {
                    contentDescription = preview
                    customActions = listOf(
                        CustomAccessibilityAction(pinLabel) { onPin(); true },
                        CustomAccessibilityAction(deleteLabel) { onRemove(); true },
                    )
                }
                .combinedClickable(onClick = onSelect, onLongClick = onPreview),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClipboardRowTextContent(entry, preview, now, palette, Modifier.weight(1f))
            ClipboardPinAction(entry.isPinned, pinLabel, palette, onPin)
        }
    }
}

internal fun shouldRevealClipboardAction(offsetPx: Float, actionWidthPx: Float): Boolean =
    offsetPx <= -actionWidthPx / 2f

@Composable
private fun DeleteAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.width(ActionWidth).fillMaxHeight().background(Color(0xFFB3261E))
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
            drawRect(Color.White, androidx.compose.ui.geometry.Offset(size.width * .25f, size.height * .3f),
                androidx.compose.ui.geometry.Size(size.width * .5f, size.height * .58f), style = Stroke(1.5.dp.toPx()))
            drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .22f),
                androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .22f), 1.5.dp.toPx())
        }
        BasicText(label, style = TextStyle(Color.White, 12.sp))
    }
}
