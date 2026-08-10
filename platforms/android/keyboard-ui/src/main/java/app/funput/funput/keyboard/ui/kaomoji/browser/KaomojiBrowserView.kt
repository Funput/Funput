package app.funput.funput.keyboard.ui.kaomoji.browser

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCatalog
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCategory
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiItem
import app.funput.funput.keyboard.ui.panel.KeyboardPanelComposeView
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import kotlinx.coroutines.flow.distinctUntilChanged

internal class KaomojiBrowserView(context: Context) : KeyboardPanelComposeView(context) {
    var onKaomojiSelected: (KaomojiItem) -> Unit = {}
    var onCategoryChanged: (KaomojiCategory) -> Unit = {}
    private var sections by mutableStateOf(KaomojiSections(emptyList()))
    private var palette by mutableStateOf<KeyboardPanelPalette?>(null)
    private var loaded by mutableStateOf(false)
    private var requestedCategory by mutableStateOf<KaomojiCategory?>(null)
    private var requestId by mutableIntStateOf(0)

    init { setContent { Content() } }

    fun submit(catalog: KaomojiCatalog, recent: List<String>) {
        sections = KaomojiSections.from(catalog, recent)
        loaded = true
    }

    fun updatePalette(value: KeyboardPanelPalette) { palette = value }

    fun scrollTo(category: KaomojiCategory) { requestedCategory = category; requestId++ }

    fun reset() { scrollTo(sections.values.firstOrNull()?.category ?: KaomojiCategory.HAPPY) }

    @Composable
    private fun Content() {
        val colors = palette ?: return
        if (sections.values.isEmpty()) return EmptyState(colors)
        val state = rememberLazyListState()
        LaunchedEffect(requestId) {
            sections.position(requestedCategory ?: return@LaunchedEffect)
                .takeIf { it >= 0 }?.let { state.scrollToItem(it) }
        }
        LaunchedEffect(state, sections) {
            snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged()
                .collect { sections.categoryAt(it)?.let(onCategoryChanged) }
        }
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(top = 8.dp, bottom = 10.dp),
        ) {
            sections.values.forEach { section ->
                item(key = section.category.name) { Section(section, colors) }
            }
        }
    }

    @Composable
    private fun Section(section: KaomojiSection, colors: KeyboardPanelPalette) {
        BasicText(
            section.category.label,
            Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 16.dp),
            TextStyle(Color(colors.secondaryLabel), fontSize = 13.sp),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val availableWidth = maxWidth - 16.dp
            FlowRow(
                Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                section.items.forEach { item -> KaomojiCell(item, availableWidth, colors) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun KaomojiCell(item: KaomojiItem, maximumWidth: androidx.compose.ui.unit.Dp, colors: KeyboardPanelPalette) {
        val style = TextStyle(Color(colors.label), fontSize = 17.sp)
        val measurer = rememberTextMeasurer()
        val measured = remember(item.text, measurer) { measurer.measure(item.text, style).size.width }
        val density = LocalDensity.current
        val width = with(density) {
            KaomojiCellMetrics.widthFor(measured.toDp().value, maximumWidth.value).dp
        }
        Box(
            Modifier.height(44.dp).width(width)
                .semantics { contentDescription = item.name }
                .clickable { onKaomojiSelected(item) }.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                item.text,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                autoSize = TextAutoSize.StepBased(10.sp, 17.sp, 1.sp),
            )
        }
    }

    @Composable
    private fun EmptyState(colors: KeyboardPanelPalette) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(
                if (loaded) "Không có kaomoji" else "Đang tải kaomoji…",
                style = TextStyle(Color(colors.secondaryLabel), fontSize = 15.sp),
            )
        }
    }
}
