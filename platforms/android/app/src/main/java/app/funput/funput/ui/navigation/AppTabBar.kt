package app.funput.funput.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.layout.FunputTabBar

/** The app's tab bar, bound to [navigator]: for screens built on `FunputScreen`'s tab bar slot. */
@Composable
internal fun AppTabBar(navigator: AppNavigator, modifier: Modifier = Modifier) {
    FunputTabBar(
        tabs = appTabs(),
        selectedIndex = navigator.currentTab.ordinal,
        onSelect = { index -> navigator.selectTab(TopLevelDestination.entries[index]) },
        modifier = modifier,
    )
}
