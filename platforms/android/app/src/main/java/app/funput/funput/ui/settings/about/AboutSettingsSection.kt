package app.funput.funput.ui.settings.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsValueRow
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange

/**
 * What is left of the old About section: the version, and the way through to the about screen.
 *
 * The links that used to sit here moved to that screen. This row goes away once the tab bar lands
 * and makes the destination reachable on its own.
 */
@Composable
internal fun AboutSettingsSection(
    versionName: String,
    onOpenAbout: () -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_about),
        rows = listOf(
            { position ->
                SettingsValueRow(
                    position = position,
                    title = stringResource(R.string.settings_version_title),
                    value = versionName,
                    iconRes = R.drawable.ic_info,
                    iconBackground = BrandBlue,
                )
            },
            { position ->
                SettingsRow(
                    position = position,
                    title = stringResource(R.string.settings_about_row_title),
                    iconRes = R.drawable.ic_heart,
                    iconBackground = BrandOrange,
                    onClick = onOpenAbout,
                )
            },
        ),
    )
}
