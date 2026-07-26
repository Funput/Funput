package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.KeyboardThemeSlot

/**
 * Chooses whether the keyboard follows the system appearance, and which slot the gallery assigns.
 *
 * A Funput theme carries one palette, so following the system means picking two themes rather
 * than one theme that adapts; the segments make it explicit which of the two is being edited.
 */
@Composable
internal fun ThemeAppearanceControls(
    followsAppearance: Boolean,
    activeSlot: KeyboardThemeSlot,
    onFollowsAppearanceChange: (Boolean) -> Unit,
    onSlotSelected: (KeyboardThemeSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.theme_gallery_follow_appearance),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.theme_gallery_follow_appearance_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = followsAppearance, onCheckedChange = onFollowsAppearanceChange)
        }
        if (followsAppearance) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SlotChip(KeyboardThemeSlot.LIGHT, activeSlot, onSlotSelected)
                SlotChip(KeyboardThemeSlot.DARK, activeSlot, onSlotSelected)
            }
        }
    }
}

@Composable
private fun SlotChip(
    slot: KeyboardThemeSlot,
    activeSlot: KeyboardThemeSlot,
    onSelected: (KeyboardThemeSlot) -> Unit,
) {
    val labelRes = if (slot == KeyboardThemeSlot.LIGHT) {
        R.string.theme_gallery_slot_light
    } else {
        R.string.theme_gallery_slot_dark
    }
    FilterChip(
        selected = slot == activeSlot,
        onClick = { onSelected(slot) },
        label = { Text(stringResource(labelRes)) },
    )
}
