package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal class EmojiResultsView(context: Context) : EmojiComposeView(context) {
    var onEmojiSelected: (EmojiItem) -> Unit = {}
    private var items by mutableStateOf(emptyList<EmojiItem>())
    private var vertical by mutableStateOf(false)

    init { setContent { EmojiResultsContent(items, vertical, onEmojiSelected) } }

    fun submit(items: List<EmojiItem>, isVertical: Boolean) {
        this.items = items
        vertical = isVertical
    }
}

@Composable
internal fun EmojiResultsContent(
    items: List<EmojiItem>,
    vertical: Boolean,
    selected: (EmojiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (vertical) {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val columns = EmojiGridMetrics.columnsFor(maxWidth.value)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                gridItems(items, key = EmojiItem::glyph) {
                    EmojiCell(it, Modifier.fillMaxWidth(), selected)
                }
            }
        }
    } else {
        LazyRow(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp)) {
            items(items, key = EmojiItem::glyph) { EmojiCell(it, selected = selected) }
        }
    }
}
