package app.funput.funput.ui.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.theme.Spacing

/** A row that only reports a value. No click, so its corners never animate. */
@Composable
internal fun SettingsValueRow(
    position: RowPosition,
    title: String,
    value: String,
    @DrawableRes iconRes: Int,
    tone: SettingsIconTone,
) {
    SettingsRowSurface(
        position = position,
        interactionSource = remember { MutableInteractionSource() },
    ) {
        SettingsIcon(iconRes, tone)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
