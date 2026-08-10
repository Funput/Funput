package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.navigation.sharedElementByKey
import app.funput.funput.ui.theme.KeyboardThemePreview
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.ui.theme.themePreviewSharedKey

/**
 * One theme, at the width of the keyboard it is previewing.
 *
 * A keyboard fills the screen edge to edge, so a preview narrower than the card it sits in has to
 * squash it to fit and stops looking like the thing being chosen. At full width the aspect matches
 * the real keyboard and the keys stay readable.
 */
@Composable
internal fun ThemeCard(
    descriptor: KeyboardThemeDescriptor,
    selected: Boolean,
    onSelected: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val title = descriptor.localizedName()
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    }
    // Material separates layers by tone first and shadow second, so the chosen theme takes both a
    // step up the surface scale and a shadow. A border alone reads as an outline drawn on a flat
    // list; lifting it says this is the one in use.
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shadowElevation = if (selected) SelectedElevation else 0.dp,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag(descriptor.id.value)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelected),
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            KeyboardThemePreview(
                theme = descriptor.theme,
                backgroundImage = descriptor.backgroundImage,
                modifier = Modifier
                    .sharedElementByKey(themePreviewSharedKey(descriptor.id))
                    .fillMaxWidth()
                    .aspectRatio(PreviewAspect)
                    .clip(MaterialTheme.shapes.small),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.Medium, start = Spacing.Tight),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.theme_gallery_author, descriptor.author),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                ThemeActions(
                    title = title,
                    selected = selected,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun KeyboardThemeDescriptor.localizedName(): String = when (id) {
    KeyboardThemeId.Dark -> stringResource(R.string.settings_keyboard_theme_dark)
    KeyboardThemeId.Light -> stringResource(R.string.settings_keyboard_theme_light)
    else -> name
}

/** Roughly the shape of the real keyboard: full width, a little under half as tall. */
private const val PreviewAspect = 2.05f

private val SelectedElevation = 3.dp
