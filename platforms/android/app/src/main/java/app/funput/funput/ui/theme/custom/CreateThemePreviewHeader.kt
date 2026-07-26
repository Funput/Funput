package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.ui.theme.KeyboardThemePreview

@Composable
internal fun CreateThemePreviewHeader(
    previewTheme: KeyboardTheme,
    backgroundImage: KeyboardThemeBackgroundImage?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CardShape,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(previewGradient())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.custom_theme_preview_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.custom_theme_preview_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            KeyboardThemePreview(
                theme = previewTheme,
                backgroundImage = backgroundImage,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(CardShape),
            )
        }
    }
}

@Composable
private fun previewGradient() = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ),
)
