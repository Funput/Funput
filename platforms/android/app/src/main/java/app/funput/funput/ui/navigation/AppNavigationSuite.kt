package app.funput.funput.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

/**
 * The three tabs, as a bar at the bottom of a phone and as a rail once the window is wide enough —
 * an unfolded foldable or a tablet, where a bottom bar would be both ugly and far from the thumb.
 * Which one appears is [NavigationSuiteScaffoldDefaults]' call, from the window size.
 *
 * The tabs disappear on any screen below a tab's root. They are for moving between tabs, and the
 * theme studio is somewhere you finish or leave, not somewhere you tab away from mid-edit.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun AppNavigationSuite(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val atTabRoot = navigator.currentDestination.depth == 0
    NavigationSuiteScaffold(
        layoutType = if (atTabRoot) {
            NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
        } else {
            NavigationSuiteType.None
        },
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { tab ->
                val ui = tab.ui
                item(
                    selected = tab == navigator.currentTab,
                    onClick = { navigator.selectTab(tab) },
                    icon = { Icon(painterResource(ui.icon), contentDescription = null) },
                    label = { Text(stringResource(ui.label)) },
                )
            }
        },
        modifier = modifier,
        content = content,
    )
}
