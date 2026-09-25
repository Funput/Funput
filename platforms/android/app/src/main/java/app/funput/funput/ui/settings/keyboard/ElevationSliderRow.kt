package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import app.funput.funput.ui.settings.components.RowPosition
import app.funput.funput.ui.settings.components.SettingsIcon
import app.funput.funput.ui.settings.components.SettingsRowSurface
import app.funput.funput.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
internal fun ElevationSliderRow(
    position: RowPosition,
    valueDp: Float,
    maximumDp: Float,
    onSettled: (Float) -> Unit,
) {
    val safeMaximum = maximumDp.coerceAtLeast(1f)
    var draft by remember(valueDp, safeMaximum) {
        mutableFloatStateOf(valueDp.coerceIn(0f, safeMaximum))
    }
    val title = stringResource(R.string.settings_elevation_title)
    SettingsRowSurface(position = position, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
        SettingsIcon(R.drawable.ic_key_size)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.settings_elevation_value, draft.roundToInt()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Slider(
                value = draft,
                onValueChange = { draft = it },
                onValueChangeFinished = { onSettled(draft.roundToInt().toFloat()) },
                valueRange = 0f..safeMaximum,
                enabled = maximumDp > 0f,
                modifier = Modifier.semantics { contentDescription = title },
            )
        }
    }
}
