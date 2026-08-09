package app.funput.funput.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.theme.KeyboardThemePreview
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.ui.theme.localizedName

/**
 * The keyboard you are actually typing on, at the top of its own settings.
 *
 * The page it opens used to start with a logo and a tagline — decoration, and decoration the about
 * tab already carried. This is the same size and position and it is the one thing on the page that
 * is neither a row nor a label: a real object, in the theme the user chose, on a page that was
 * otherwise four identical lists of text.
 */
@Composable
internal fun KeyboardHero(
    descriptor: KeyboardThemeDescriptor,
    onOpenAppearance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenAppearance),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.padding(Spacing.Medium),
        ) {
            KeyboardThemePreview(
                theme = descriptor.theme,
                backgroundImage = descriptor.backgroundImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PreviewAspect)
                    .clip(MaterialTheme.shapes.small),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = Spacing.Tight),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = descriptor.localizedName(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_keyboard_hero_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Roughly the shape of the real keyboard: full width, a little under half as tall. */
private const val PreviewAspect = 2.05f
