package app.funput.funput.keyboard.ui.clipboard.row

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.clipboard.ClipboardRowText
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import java.time.Instant
import kotlin.math.roundToInt

/** Gap between a history card and the side of the panel. */
internal val ClipboardCardInset = 8.dp

/** Gap between two history cards, and between a card and its section header. */
internal val ClipboardCardSpacing = 6.dp

/** Width of the delete action a swipe uncovers at the end of a card. */
internal val ClipboardDeleteWidth = 88.dp

private val CardMinHeight = 48.dp
private val CardShape = RoundedCornerShape(10.dp)

/**
 * One history entry as a card: tap pastes, long press previews, and a swipe towards the start
 * slides the card aside to uncover its delete action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ClipboardSwipeRow(
    entry: KeyboardClipboardEntry, now: Instant, palette: KeyboardPanelPalette,
    revealed: Boolean, onReveal: (Boolean) -> Unit, onSelect: () -> Unit,
    onPin: () -> Unit, onRemove: () -> Unit, onPreview: () -> Unit,
) {
    val widthPx = with(LocalDensity.current) { ClipboardDeleteWidth.toPx() }
    var dragging by remember(entry.id) { mutableStateOf(false) }
    var dragOffset by remember(entry.id) { mutableFloatStateOf(0f) }
    val target = if (revealed) -widthPx else 0f
    val animated by animateFloatAsState(target, label = "clipboard swipe")
    val offset = if (dragging) dragOffset else animated
    val preview = ClipboardRowText.preview(entry.text)
    val pinLabel = stringResource(if (entry.isPinned) R.string.clipboard_unpin else R.string.clipboard_pin)
    val deleteLabel = stringResource(R.string.clipboard_delete)
    Box(Modifier.fillMaxWidth().padding(horizontal = ClipboardCardInset).clip(CardShape)) {
        ClipboardDeleteAction(
            deleteLabel, uncovered = { -offset }, onClick = { onReveal(false); onRemove() },
            modifier = Modifier.matchParentSize(),
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = CardMinHeight)
                .offset { IntOffset(offset.roundToInt(), 0) }
                .background(Color(palette.cardSurface))
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
            ClipboardRowLabel(entry, preview, now, palette, Modifier.weight(1f))
            ClipboardPinAction(entry.isPinned, pinLabel, palette, onPin)
        }
    }
}

internal fun shouldRevealClipboardAction(offsetPx: Float, actionWidthPx: Float): Boolean =
    offsetPx <= -actionWidthPx / 2f
