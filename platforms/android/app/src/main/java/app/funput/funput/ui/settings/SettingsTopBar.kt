package app.funput.funput.ui.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R

/**
 * The settings title, as a collapsing app bar rather than the first row of the list.
 *
 * A list item cannot do what this does: shrink into a compact bar as the page scrolls, tint itself
 * once content passes underneath, and hold the status bar area while the list scrolls behind it.
 * This is Android's answer to a large title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTopBar(scrollBehavior: TopAppBarScrollBehavior) {
    LargeTopAppBar(
        // No subtitle: the hero card right below already carries the name and the tagline.
        title = { Text(text = stringResource(R.string.settings_title)) },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/** Collapses the title on scroll down and only brings it back at the top of the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSettingsScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
