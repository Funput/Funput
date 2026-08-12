package app.funput.funput.keyboard.ui.clipboard
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import java.time.Instant
import kotlin.math.roundToInt
@Composable
internal fun ClipboardPanelContent(
    entries: List<KeyboardClipboardEntry>, loading: Boolean, now: Instant,
    palette: KeyboardPanelPalette, reset: Int,
    onSelect: (KeyboardClipboardEntry) -> Unit, onPin: (KeyboardClipboardEntry) -> Unit,
    onRemove: (KeyboardClipboardEntry) -> Unit, onClear: () -> Unit,
    onDelete: () -> Unit, onReturn: () -> Unit,
) {
    var preview by remember { mutableStateOf<KeyboardClipboardEntry?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(reset) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when { loading -> Empty(stringResource(R.string.clipboard_loading), null, palette)
                entries.isEmpty() -> Empty(
                    stringResource(R.string.clipboard_empty_title),
                    stringResource(R.string.clipboard_empty_detail), palette,
                )
                else -> LazyColumn(state = listState) {
                    clipboardGroups(entries).forEach { group ->
                        stickyHeader(group.pinned) { Header(group.pinned, palette) }
                        items(group.entries.size, key = { group.entries[it].id }) { index ->
                            ClipboardRow(group.entries[index], now, palette, onSelect, onPin, onRemove) {
                                preview = it
                            }
                        }
                    }
                }
            }
            preview?.let { entry -> ClipboardPreviewOverlay(
                entry, palette, { preview = null },
                { preview = null; onPin(entry) }, { preview = null; onRemove(entry) },
            ) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(palette.divider)))
        Box(Modifier.fillMaxWidth().height(46.dp)) {
            ClipboardBottomBar(palette, entries.isNotEmpty(), onReturn, onClear, onDelete)
        }
    }
}
@Composable
private fun Header(pinned: Boolean, palette: KeyboardPanelPalette) {
    BasicText(
        stringResource(if (pinned) R.string.clipboard_pinned else R.string.clipboard_recent),
        Modifier.fillMaxWidth().background(Color(palette.backgroundStart)).padding(12.dp, 5.dp),
        TextStyle(Color(palette.readable(palette.secondaryLabel)), 12.sp, fontWeight = FontWeight.Medium),
    )
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipboardRow(
    entry: KeyboardClipboardEntry, now: Instant, palette: KeyboardPanelPalette,
    onSelect: (KeyboardClipboardEntry) -> Unit, onPin: (KeyboardClipboardEntry) -> Unit,
    onRemove: (KeyboardClipboardEntry) -> Unit, onPreview: (KeyboardClipboardEntry) -> Unit,
) {
    var offset by remember(entry.id) { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val preview = ClipboardRowText.preview(entry.text)
    val pinLabel = stringResource(if (entry.isPinned) R.string.clipboard_unpin else R.string.clipboard_pin)
    val deleteLabel = stringResource(R.string.clipboard_delete)
    Box(Modifier.fillMaxWidth().height(44.dp)) {
        if (offset < 0f) Box(Modifier.fillMaxSize().background(Color(0xFFD32F2F)))
        Row(
            Modifier.fillMaxSize().offset { IntOffset(offset.roundToInt(), 0) }
                .then(if (offset < 0f) Modifier.background(Color(palette.backgroundEnd)) else Modifier)
                .pointerInput(entry.id) { detectHorizontalDragGestures(
                    onDragEnd = { if (offset <= -threshold) onRemove(entry); offset = 0f },
                ) { change, amount -> change.consume(); offset = (offset + amount).coerceIn(-threshold * 1.4f, 0f) } }
                .semantics { contentDescription = preview; customActions = listOf(
                    CustomAccessibilityAction(pinLabel) { onPin(entry); true },
                    CustomAccessibilityAction(deleteLabel) { onRemove(entry); true },
                ) }
                .combinedClickable(onClick = { onSelect(entry) }, onLongClick = { onPreview(entry) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowText(entry, preview, now, palette, Modifier.weight(1f))
            ClipboardPinAction(entry.isPinned, pinLabel, palette) { onPin(entry) }
        }
    }
}
@Composable
private fun RowText(entry: KeyboardClipboardEntry, preview: String, now: Instant, palette: KeyboardPanelPalette, modifier: Modifier) {
    val resources = LocalContext.current.resources
    val strings = ClipboardTimeStrings(
        stringResource(R.string.clipboard_just_now),
        { resources.getString(R.string.clipboard_minutes_ago, it) },
        { resources.getString(R.string.clipboard_hours_ago, it) },
        { resources.getString(R.string.clipboard_days_ago, it) },
    )
    Column(modifier.padding(start = 12.dp)) {
        BasicText(preview, style = TextStyle(Color(palette.readable(palette.label)), 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(ClipboardRowText.relativeTime(entry.capturedAt, now, strings), style = TextStyle(Color(palette.readable(palette.secondaryLabel)), 11.sp))
    }
}
@Composable private fun Empty(title: String, detail: String?, palette: KeyboardPanelPalette) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        BasicText(title, Modifier.fillMaxWidth(), TextStyle(Color(palette.readable(palette.label)), 15.sp,
            fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
        detail?.let { BasicText(it, Modifier.fillMaxWidth().padding(top = 6.dp),
            TextStyle(Color(palette.readable(palette.secondaryLabel)), 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)) }
    }
}
