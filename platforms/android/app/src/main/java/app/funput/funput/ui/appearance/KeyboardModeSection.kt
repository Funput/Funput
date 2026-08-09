package app.funput.funput.ui.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.ui.settings.components.SettingsGroup
import app.funput.funput.ui.settings.components.SettingsSectionHeader
import app.funput.funput.ui.settings.components.SettingsIconTone
import app.funput.funput.ui.settings.components.SettingsSwitchRow

/**
 * Whether the keyboard carries one theme or two, and — when it carries two — which of them the
 * gallery below is currently assigning to.
 *
 * The chips name the theme in each slot rather than just the slot. Tapping a card means different
 * things depending on which chip is active, and a control that changes what a tap means has to say
 * so where the eye already is, not only in its own label.
 */
@Composable
internal fun KeyboardModeSection(
    followsAppearance: Boolean,
    activeSlot: KeyboardThemeSlot,
    lightThemeName: String,
    darkThemeName: String,
    onFollowsAppearanceChange: (Boolean) -> Unit,
    onSlotSelected: (KeyboardThemeSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(stringResource(R.string.appearance_section_keyboard))
        SettingsGroup(
            rows = listOf { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.theme_gallery_follow_appearance),
                    summary = stringResource(R.string.theme_gallery_follow_appearance_description),
                    checked = followsAppearance,
                    iconRes = R.drawable.ic_appearance,
                    tone = SettingsIconTone.Primary,
                    onCheckedChange = onFollowsAppearanceChange,
                )
            },
        )
        if (followsAppearance) {
            Text(
                text = stringResource(R.string.appearance_slot_assigning),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SlotChip(KeyboardThemeSlot.LIGHT, activeSlot, lightThemeName, onSlotSelected)
                SlotChip(KeyboardThemeSlot.DARK, activeSlot, darkThemeName, onSlotSelected)
            }
        }
    }
}

@Composable
private fun SlotChip(
    slot: KeyboardThemeSlot,
    activeSlot: KeyboardThemeSlot,
    themeName: String,
    onSelected: (KeyboardThemeSlot) -> Unit,
) {
    val labelRes = if (slot == KeyboardThemeSlot.LIGHT) {
        R.string.appearance_slot_light_named
    } else {
        R.string.appearance_slot_dark_named
    }
    FilterChip(
        selected = slot == activeSlot,
        onClick = { onSelected(slot) },
        label = { Text(stringResource(labelRes, themeName)) },
    )
}
