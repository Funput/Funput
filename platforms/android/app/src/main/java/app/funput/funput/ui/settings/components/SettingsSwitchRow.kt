package app.funput.funput.ui.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun SettingsSwitchRow(
    position: RowPosition,
    title: String,
    checked: Boolean,
    @DrawableRes iconRes: Int,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    SettingsRowSurface(
        position = position,
        interactionSource = interactionSource,
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            role = Role.Switch,
            onValueChange = { value ->
                // The switch animates over a couple of hundred milliseconds; the tick lands now,
                // so the toggle feels like it answered the finger rather than the animation.
                haptics.performHapticFeedback(
                    if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                )
                onCheckedChange(value)
            },
        ),
    ) {
        SettingsIcon(iconRes)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            summary?.let { text ->
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
