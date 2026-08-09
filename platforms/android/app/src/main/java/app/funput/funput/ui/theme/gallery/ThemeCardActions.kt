package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.PillShape

/**
 * The badge for the theme in use, and — for a theme the user made — an overflow menu.
 *
 * Editing and deleting used to be two text buttons on the face of every custom card. That put a
 * destructive action one tap from a card whose whole job is to be tapped, and it crowded out the
 * name it sat next to.
 */
@Composable
internal fun ThemeActions(
    title: String,
    selected: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (selected) SelectedBadge()
        if (onEdit != null || onDelete != null) {
            Spacer(modifier = Modifier.width(4.dp))
            ThemeOverflowMenu(title, onEdit, onDelete)
        }
    }
}

@Composable
private fun ThemeOverflowMenu(title: String, onEdit: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.theme_gallery_more_description, title)
    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        onEdit?.let { edit ->
            MenuAction(R.string.theme_gallery_edit, editDescription(title), MaterialTheme.colorScheme.onSurface) {
                expanded = false
                edit()
            }
        }
        onDelete?.let { delete ->
            MenuAction(R.string.theme_gallery_delete, deleteDescription(title), MaterialTheme.colorScheme.error) {
                expanded = false
                delete()
            }
        }
    }
}

@Composable
private fun MenuAction(
    labelRes: Int,
    description: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(labelRes), color = color) },
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun editDescription(title: String) =
    stringResource(R.string.theme_gallery_edit_description, title)

@Composable
private fun deleteDescription(title: String) =
    stringResource(R.string.theme_gallery_delete_description, title)

@Composable
private fun SelectedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = PillShape,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(stringResource(R.string.theme_gallery_selected), style = MaterialTheme.typography.labelLarge)
        }
    }
}
