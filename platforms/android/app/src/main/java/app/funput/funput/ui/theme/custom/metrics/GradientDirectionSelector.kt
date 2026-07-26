package app.funput.funput.ui.theme.custom.metrics

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeGradientDirection

@Composable
internal fun GradientDirectionSelector(
    selected: KeyboardThemeGradientDirection,
    onSelected: (KeyboardThemeGradientDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.custom_theme_metric_gradient_direction),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        KeyboardThemeGradientDirection.entries.forEach { direction ->
            FilterChip(
                selected = direction == selected,
                onClick = { onSelected(direction) },
                label = { Text(stringResource(direction.labelRes)) },
            )
        }
    }
}

@get:StringRes
private val KeyboardThemeGradientDirection.labelRes: Int
    get() = when (this) {
        KeyboardThemeGradientDirection.HORIZONTAL -> R.string.custom_theme_gradient_horizontal
        KeyboardThemeGradientDirection.VERTICAL -> R.string.custom_theme_gradient_vertical
        KeyboardThemeGradientDirection.DIAGONAL_DOWN -> R.string.custom_theme_gradient_diagonal_down
        KeyboardThemeGradientDirection.DIAGONAL_UP -> R.string.custom_theme_gradient_diagonal_up
    }
