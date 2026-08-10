package app.funput.funput.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        // Uppercase and tracked out. It is the difference between a page that was typed and a page
        // that was set, and it costs two properties.
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = SectionTracking,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = Spacing.Large, bottom = Spacing.Small),
    )
}

private val SectionTracking = 1.2.sp

/** Section title stays outside; the rows below it form one [SettingsGroup]. */
@Composable
internal fun SettingsSection(
    title: String,
    rows: List<SettingsRowContent>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(title)
        SettingsGroup(rows)
    }
}
