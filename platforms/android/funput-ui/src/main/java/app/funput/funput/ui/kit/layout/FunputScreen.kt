package app.funput.funput.ui.kit.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.glass.LocalGlassBackdrop
import app.funput.funput.ui.kit.glass.glassSource
import app.funput.funput.ui.kit.glass.rememberGlassBackdrop
import app.funput.funput.ui.kit.theme.FunputUi

/** Test tag of the scrolling list inside every [FunputScreen]. */
const val FunputScreenListTag: String = "funput-screen-list"

/** How far the large title scrolls before the compact title takes over. */
private val CollapseDistance = 40.dp

/**
 * A FunputUI screen: a large title that scrolls with the content, a top bar that turns to glass
 * and shows a compact title once the content passes under it, an optional floating [tabBar], and
 * a centred column never wider than the layout token allows.
 *
 * [content] is a lazy list of sections (typically `FunputSection`s), spaced by the section gap.
 * The screen owns the glass backdrop its bars sample, so bars placed here need no wiring.
 */
@Composable
fun FunputScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    tabBar: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val backdrop = rememberGlassBackdrop()
    val spacing = FunputUi.spacing
    val collapsePx = with(LocalDensity.current) { CollapseDistance.roundToPx() }
    val collapsed by remember(listState, collapsePx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapsePx
        }
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        if (tabBar != null) TabBarReserve else spacing.section
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(backdrop)
                    .background(FunputUi.colors.groupedBackground),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = spacing.pageMargin,
                        end = spacing.pageMargin,
                        top = top,
                        bottom = bottom,
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.section),
                    modifier = Modifier
                        .widthIn(max = spacing.contentMaxWidth)
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .testTag(FunputScreenListTag),
                ) {
                    item(key = "funput-large-title") { LargeTitle(title) }
                    content()
                }
            }
            FunputTopBar(
                title = title,
                collapsed = collapsed,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            tabBar?.let { bar -> Box(Modifier.align(Alignment.BottomCenter)) { bar() } }
        }
    }
}

@Composable
private fun LargeTitle(title: String) {
    BasicText(
        text = title,
        style = FunputUi.typography.display.copy(color = FunputUi.colors.label),
        modifier = Modifier.semantics { heading() },
    )
}
