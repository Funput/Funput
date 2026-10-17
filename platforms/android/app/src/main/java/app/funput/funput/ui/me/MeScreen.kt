package app.funput.funput.ui.me

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.about.AboutFooter
import app.funput.funput.ui.about.AboutHero
import app.funput.funput.ui.about.AboutLink
import app.funput.funput.ui.about.AboutLinks
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.hub.SettingsHubRow
import app.funput.funput.ui.settings.hub.SettingsNavigationCard
import app.funput.funput.ui.settings.pages.SettingsPage

/**
 * The "Tôi" tab: the data Funput keeps on this device, then everything about Funput itself, where
 * its source lives, how to reach the people behind it, and the licences it ships under. Links that
 * leave the app say so with their arrow.
 */
@Composable
internal fun MeScreen(
    versionName: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenData: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    tabBar: (@Composable () -> Unit)? = null,
) {
    FunputScreen(title = stringResource(R.string.nav_me), modifier = modifier, tabBar = tabBar) {
        item(key = "hero") { AboutHero(versionName) }
        MeSection.entries.forEach { section ->
            when (section) {
                MeSection.DATA -> item(key = section.name) {
                    SettingsNavigationCard(
                        title = null,
                        rows = listOf(SettingsHubRow(SettingsPage.DATA, summary = null)),
                        onOpenPage = { onOpenData() },
                        modifier = Modifier.testTag(MeDataTag),
                    )
                }
                MeSection.DISCOVERY ->
                    linkSection(section, R.string.about_section_discovery, AboutLinks.discovery, onOpenLink)
                MeSection.SUPPORT ->
                    linkSection(section, R.string.about_section_support, AboutLinks.support, onOpenLink)
                MeSection.LEGAL ->
                    linkSection(section, R.string.about_section_privacy, AboutLinks.legal, onOpenLink)
                MeSection.LICENSES -> item(key = section.name) {
                    FunputSection(title = null) {
                        LinkRow(
                            title = stringResource(R.string.licenses_title),
                            summary = stringResource(R.string.licenses_summary),
                            onClick = onOpenLicenses,
                            icon = FunputIcons.Licenses,
                            tint = FunputTint.GRAY,
                        )
                    }
                }
            }
        }
        item(key = "footer") { AboutFooter() }
    }
}

private fun LazyListScope.linkSection(
    section: MeSection,
    @StringRes titleRes: Int,
    links: List<AboutLink>,
    onOpenLink: (String) -> Unit,
) = item(key = section.name) {
    FunputSection(title = stringResource(titleRes)) {
        links.forEachIndexed { index, link ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            val url = stringResource(link.url)
            LinkRow(
                title = stringResource(link.title),
                summary = stringResource(link.summary),
                onClick = { onOpenLink(url) },
                icon = link.icon,
                tint = link.tint,
                external = true,
            )
        }
    }
}

/** Test tag of the row that opens the data page. */
internal const val MeDataTag = "me-data"
