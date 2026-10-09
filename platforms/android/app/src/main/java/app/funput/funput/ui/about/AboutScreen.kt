package app.funput.funput.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.theme.FunputTint

/**
 * Everything about Funput itself: what it is, where its source lives, how to reach the people
 * behind it, and the licences it ships under. Links that leave the app say so with their arrow.
 */
@Composable
internal fun AboutScreen(
    versionName: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenLicenses: () -> Unit = {},
    tabBar: (@Composable () -> Unit)? = null,
) {
    FunputScreen(title = stringResource(R.string.about_title), modifier = modifier, tabBar = tabBar) {
        item(key = "hero") { AboutHero(versionName) }
        linkSection("discovery", R.string.about_section_discovery, AboutLinks.discovery, onOpenLink)
        linkSection("support", R.string.about_section_support, AboutLinks.support, onOpenLink)
        linkSection("legal", R.string.about_section_privacy, AboutLinks.legal, onOpenLink)
        item(key = "licenses") {
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
        item(key = "footer") { AboutFooter() }
    }
}

private fun LazyListScope.linkSection(
    key: String,
    @StringRes titleRes: Int,
    links: List<AboutLink>,
    onOpenLink: (String) -> Unit,
) = item(key = key) {
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
