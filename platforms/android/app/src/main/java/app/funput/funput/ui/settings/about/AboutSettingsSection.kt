package app.funput.funput.ui.settings.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsDivider
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsValueRow
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange

@Composable
internal fun AboutSettingsSection(
    versionName: String,
    onOpenWebsite: () -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_about)) {
        SettingsValueRow(
            title = stringResource(R.string.settings_version_title),
            value = versionName,
            iconRes = R.drawable.ic_info,
            iconBackground = BrandBlue,
        )
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.settings_website_title),
            value = stringResource(R.string.settings_website_display),
            iconRes = R.drawable.ic_globe,
            iconBackground = BrandOrange,
            iconColor = Color(0xFF3D2200),
            onClick = onOpenWebsite,
        )
    }
}
