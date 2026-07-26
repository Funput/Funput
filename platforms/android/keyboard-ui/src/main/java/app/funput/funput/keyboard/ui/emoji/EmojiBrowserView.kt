package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

internal class EmojiBrowserView(context: Context) : EmojiComposeView(context) {
    var onEmojiSelected: (EmojiItem) -> Unit = {}
    var onCategoryChanged: (EmojiCategory) -> Unit = {}
    private var sections by mutableStateOf(EmojiSections(emptyList()))
    private var palette by mutableStateOf<EmojiPanelPalette?>(null)
    private var requestedCategory by mutableStateOf<EmojiCategory?>(null)
    private var requestId by mutableIntStateOf(0)

    init { setContent { Content() } }

    fun submit(catalog: EmojiCatalog, recents: List<String>) {
        sections = EmojiSections.from(catalog, recents)
    }

    fun updatePalette(value: EmojiPanelPalette) { palette = value }

    fun scrollTo(category: EmojiCategory) {
        requestedCategory = category
        requestId++
    }

    @androidx.compose.runtime.Composable
    private fun Content() {
        val colors = palette ?: return
        val state = rememberLazyGridState()
        LaunchedEffect(requestId) {
            sections.position(requestedCategory ?: return@LaunchedEffect)
                .takeIf { it >= 0 }?.let { state.scrollToItem(it) }
        }
        LaunchedEffect(state, sections) {
            snapshotFlow { state.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { sections.categoryAt(it)?.let(onCategoryChanged) }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = EmojiGridMetrics.columnsFor(maxWidth.value)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = state,
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 10.dp),
            ) {
                sections.values.forEach { section ->
                    item(
                        key = "header-${section.category.name}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        BasicText(
                            section.category.label,
                            Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 8.dp),
                            TextStyle(Color(colors.secondaryLabel), fontSize = 13.sp),
                        )
                    }
                    items(section.items, key = { "${section.category.name}:${it.glyph}" }) {
                        EmojiCell(it, Modifier.fillMaxWidth(), onEmojiSelected)
                    }
                }
            }
        }
    }
}
