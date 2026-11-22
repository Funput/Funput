package app.funput.funput.keyboard.ui.clipboard.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.clipboard.ClipboardDestructiveRed
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

/** The whole text of one history entry over a scrim, with its pin and delete actions. */
@Composable
internal fun ClipboardPreviewOverlay(
    entry: KeyboardClipboardEntry,
    palette: KeyboardPanelPalette,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRemove: () -> Unit,
) {
    val surface = palette.searchSurface
    val label = palette.readableOn(surface, palette.label)
    val description = stringResource(R.string.clipboard_preview)
    Box(
        Modifier.fillMaxSize().background(Scrim)
            .semantics { contentDescription = description }
            // The scrim spans the panel, so press feedback on it would flash the whole panel.
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(20.dp).clip(CardShape).background(Color(surface))
                // A tap on the card must not reach the scrim and dismiss it. The card is not a
                // button, so it takes the tap without click semantics or press feedback.
                .pointerInput(Unit) { detectTapGestures {} }
                .semantics(mergeDescendants = true) {}
                .padding(16.dp),
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
                    palette.readableOn(surface, ClipboardDestructiveRed),
                    onRemove,
                )
            }
        }
    }
}

@Composable
private fun PreviewAction(label: String, color: Int, onClick: () -> Unit) {
    BasicText(
        label,
        Modifier.padding(end = 12.dp).clip(ActionShape).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        TextStyle(Color(color), 14.sp, fontWeight = FontWeight.Medium),
    )
}

private val Scrim = Color.Black.copy(alpha = 0.52f)
private val CardShape = RoundedCornerShape(12.dp)
private val ActionShape = RoundedCornerShape(8.dp)
