package app.funput.funput.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.layout.FunputTabBar
import app.funput.funput.ui.kit.theme.FunputUi

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

/**
 * Puts the tab bar under a tab root that is not yet built on FunputUI.
 *
 * Transitional: a `FunputScreen` hosts [AppTabBar] itself, drawn as glass over its own content.
 * An older screen cannot, so it gets the bar beneath it on a solid band instead, and does not pad
 * for the navigation bar, which the tab bar now sits on. Remove this once the last legacy tab
 * root has been rebuilt.
 */
@Composable
internal fun LegacyTabHost(navigator: AppNavigator, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(FunputUi.colors.groupedBackground)) {
        Box(Modifier.weight(1f).fillMaxWidth().consumeWindowInsets(WindowInsets.navigationBars)) { content() }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppTabBar(navigator) }
    }
}
