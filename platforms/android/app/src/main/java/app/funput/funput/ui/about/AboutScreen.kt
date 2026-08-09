package app.funput.funput.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.theme.EntryTracker
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.ui.theme.rememberEntryTracker
import app.funput.funput.ui.theme.staggeredEntry
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsLinkRow
import app.funput.funput.ui.settings.components.SettingsRowContent
import app.funput.funput.ui.settings.components.SettingsSection

/**
 * Everything about Funput itself: what it is, where its source lives, and how to reach the people
 * behind it. The same three groups the iOS app shows, drawn in Funput's own Material rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(
    versionName: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        val tracker = rememberEntryTracker()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.Section),
            contentPadding = PaddingValues(
                start = Spacing.Large,
                end = Spacing.Large,
                top = padding.calculateTopPadding() + Spacing.Tight,
                bottom = padding.calculateBottomPadding() + Spacing.Section,
            ),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            item(key = "hero") {
                Box(modifier = Modifier.staggeredEntry(0, tracker)) { AboutHero(versionName) }
            }
            linkSection("discovery", R.string.about_section_discovery, AboutLinks.discovery, onOpenLink, 1, tracker)
            linkSection("support", R.string.about_section_support, AboutLinks.support, onOpenLink, 2, tracker)
            linkSection("legal", R.string.about_section_privacy, AboutLinks.legal, onOpenLink, 3, tracker)
            item(key = "footer") {
                Box(modifier = Modifier.staggeredEntry(4, tracker)) { AboutFooter() }
            }
        }
    }
}

private fun LazyListScope.linkSection(
    key: String,
    titleRes: Int,
    links: List<AboutLink>,
    onOpenLink: (String) -> Unit,
    index: Int,
    tracker: EntryTracker,
) = item(key = key) {
    SettingsSection(
        title = stringResource(titleRes),
        rows = links.map { link -> linkRow(link, onOpenLink) },
        modifier = Modifier.staggeredEntry(index, tracker),
    )
}

private fun linkRow(link: AboutLink, onOpenLink: (String) -> Unit): SettingsRowContent =
    { position ->
        val url = stringResource(link.url)
        SettingsLinkRow(
            position = position,
            title = stringResource(link.title),
            summary = stringResource(link.summary),
            iconRes = link.icon,
            tone = link.tone,
            onClick = { onOpenLink(url) },
        )
    }
