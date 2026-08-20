package app.funput.funput.ui.settings.smart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.theme.EntryTracker
import app.funput.funput.ui.theme.staggeredEntry

@Composable
internal fun GestureSettingsSection(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_gestures),
        rows = listOf { position ->
            SettingsSwitchRow(
                position = position,
                title = stringResource(R.string.settings_smart_gestures_title),
                summary = stringResource(R.string.settings_smart_gestures_summary),
                checked = enabled,
                iconRes = R.drawable.ic_keyboard,
                onCheckedChange = onEnabledChanged,
            )
        },
    )
}

internal fun LazyListScope.gestureSettingsItem(
    enabled: Boolean,
    tracker: EntryTracker,
    onEnabledChanged: (Boolean) -> Unit,
) {
    item(key = "gestures") {
        Box(modifier = Modifier.staggeredEntry(6, tracker)) {
            GestureSettingsSection(enabled = enabled, onEnabledChanged = onEnabledChanged)
        }
    }
}
