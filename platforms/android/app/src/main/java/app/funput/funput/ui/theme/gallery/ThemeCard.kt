package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.KeyboardThemePreview

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
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    }
    Surface(
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .testTag(descriptor.id.value)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelected,
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            KeyboardThemePreview(
                theme = descriptor.theme,
                backgroundImage = descriptor.backgroundImage,
                modifier = Modifier
                    .height(190.dp)
                    .clip(PreviewShape),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
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

private val CardShape = RoundedCornerShape(20.dp)
private val PreviewShape = RoundedCornerShape(14.dp)
