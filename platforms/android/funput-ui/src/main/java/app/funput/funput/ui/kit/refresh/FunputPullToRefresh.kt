package app.funput.funput.ui.kit.refresh

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.funput.funput.ui.kit.layout.TopBarHeight
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Pull down to refresh, around a `FunputScreen`. Material's pull-to-refresh, coloured from the
 * FunputUI tokens, with its spinner dropping in under the glass top bar rather than behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunputPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FunputUi.colors
    val state = rememberPullToRefreshState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier.fillMaxSize().testTag(FunputPullToRefreshTag),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                containerColor = colors.cardBackground,
                color = colors.accent,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = top),
            )
        },
        content = content,
    )
}

/** Test tag of the pull-to-refresh container. */
const val FunputPullToRefreshTag = "funput-pull-to-refresh"
