package app.funput.funput.keyboard.ui.clipboard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.clipboard.row.ClipboardSwipeRow
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import java.time.Instant
@Composable
internal fun ClipboardPanelContent(
    entries: List<KeyboardClipboardEntry>, loading: Boolean, now: Instant,
    palette: KeyboardPanelPalette, reset: Int,
    onSelect: (KeyboardClipboardEntry) -> Unit, onPin: (KeyboardClipboardEntry) -> Unit,
    onRemove: (KeyboardClipboardEntry) -> Unit, onClear: () -> Unit,
    onDelete: () -> Unit, onReturn: () -> Unit,
) {
    var preview by remember { mutableStateOf<KeyboardClipboardEntry?>(null) }
    var revealedId by remember { mutableStateOf<java.util.UUID?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(reset) { listState.scrollToItem(0); revealedId = null }
    LaunchedEffect(entries) {
        if (entries.none { it.id == revealedId }) revealedId = null
    }
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
                            val entry = group.entries[index]
                            ClipboardSwipeRow(
                                entry, now, palette, revealedId == entry.id,
                                onReveal = { reveal ->
                                    revealedId = if (reveal) entry.id else revealedId.takeUnless { it == entry.id }
                                },
                                onSelect = { if (revealedId == null) onSelect(entry) else revealedId = null },
                                onPin = { onPin(entry) }, onRemove = { onRemove(entry) },
                                onPreview = {
                                    if (revealedId == null) preview = entry else revealedId = null
                                },
                            )
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
@Composable private fun Empty(title: String, detail: String?, palette: KeyboardPanelPalette) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        BasicText(title, Modifier.fillMaxWidth(), TextStyle(Color(palette.readable(palette.label)), 15.sp,
            fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
        detail?.let { BasicText(it, Modifier.fillMaxWidth().padding(top = 6.dp),
            TextStyle(Color(palette.readable(palette.secondaryLabel)), 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)) }
    }
}
