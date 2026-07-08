package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import kotlin.math.roundToInt

@Composable
internal fun BackgroundImagePlaceholder(
    imageSelected: Boolean,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    onChooseImage: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomThemeSection(title = stringResource(R.string.custom_theme_background_title), modifier = modifier) {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(
                        if (imageSelected) {
                            R.string.custom_theme_background_selected
                        } else {
                            R.string.custom_theme_background_empty
                        },
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    if (imageSelected) {
                        OutlinedButton(onClick = onChooseImage) {
                            Text(stringResource(R.string.custom_theme_background_change))
                        }
                        TextButton(onClick = onRemoveImage) {
                            Text(stringResource(R.string.custom_theme_background_remove))
                        }
                    } else {
                        Button(onClick = onChooseImage) {
                            Text(stringResource(R.string.custom_theme_background_choose))
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.custom_theme_background_opacity, (opacity * 100).roundToInt()),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = opacity,
                    enabled = imageSelected,
                    onValueChange = onOpacityChange,
                )
            }
        }
    }
}
