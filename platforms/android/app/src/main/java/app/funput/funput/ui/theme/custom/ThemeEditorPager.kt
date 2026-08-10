package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.theme.KeyboardThemeDescriptor
import kotlinx.coroutines.launch

/**
 * Tabs over swipeable pages, the way this platform states the same idea the iOS editor states with
 * a segmented picker over paged content: tap a page or swipe to the next one.
 */
@Composable
internal fun ThemeEditorPager(
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    onOpenBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = ThemeEditorTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(tab.titleRes)) },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ThemeEditorPage(
                tab = tabs[page],
                state = state,
                baseThemes = baseThemes,
                onOpenBackground = onOpenBackground,
            )
        }
    }
}
