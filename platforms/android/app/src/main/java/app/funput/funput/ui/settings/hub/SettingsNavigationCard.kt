package app.funput.funput.ui.settings.hub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.settings.pages.SettingsPage

/** One row of the hub: a page and the sentence saying what it is set to. */
internal class SettingsHubRow(val page: SettingsPage, val summary: String?)

/**
 * A card of page rows: icon, title, the page's summary in one or two lines, and a chevron. The hub
 * lists every page this way, and "Tôi" uses it for the data page.
 */
@Composable
internal fun SettingsNavigationCard(
    title: String?,
    rows: List<SettingsHubRow>,
    onOpenPage: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    FunputSection(title = title, modifier = modifier) {
        rows.forEachIndexed { index, row ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            LinkRow(
                title = stringResource(row.page.title),
                onClick = { onOpenPage(row.page) },
                summary = row.summary,
                summaryMaxLines = 2,
                icon = row.page.icon,
                tint = row.page.tint,
                modifier = Modifier.testTag(settingsPageRowTag(row.page)),
            )
        }
    }
}

/** Test tag of the row that opens [page]. */
internal fun settingsPageRowTag(page: SettingsPage): String = "settings-page-${page.name.lowercase()}"
