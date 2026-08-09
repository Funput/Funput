package app.funput.funput.ui.about

import androidx.compose.foundation.layout.Arrangement
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
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            item(key = "hero") { AboutHero(versionName) }
            linkSection("discovery", R.string.about_section_discovery, AboutLinks.discovery, onOpenLink)
            linkSection("support", R.string.about_section_support, AboutLinks.support, onOpenLink)
            linkSection("legal", R.string.about_section_privacy, AboutLinks.legal, onOpenLink)
            item(key = "footer") { AboutFooter() }
        }
    }
}

private fun LazyListScope.linkSection(
    key: String,
    titleRes: Int,
    links: List<AboutLink>,
    onOpenLink: (String) -> Unit,
) = item(key = key) {
    SettingsSection(
        title = stringResource(titleRes),
        rows = links.map { link -> linkRow(link, onOpenLink) },
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
            iconBackground = link.tint,
            onClick = { onOpenLink(url) },
        )
    }
