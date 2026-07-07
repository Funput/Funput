package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import kotlin.math.roundToInt

@Composable
internal fun KeyBackgroundOpacitySelector(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomThemeSection(title = stringResource(R.string.custom_theme_key_background_title), modifier = modifier) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.custom_theme_key_background_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.custom_theme_key_background_opacity,
                        (opacity * 100).roundToInt(),
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = opacity,
                    valueRange = MinKeyBackgroundOpacity..MaxKeyBackgroundOpacity,
                    onValueChange = onOpacityChange,
                )
            }
        }
    }
}
