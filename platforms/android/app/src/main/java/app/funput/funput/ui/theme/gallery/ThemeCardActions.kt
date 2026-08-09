package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.PillShape

@Composable
internal fun ThemeActions(
    title: String,
    selected: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        onEdit?.let {
            ActionButton(R.string.theme_gallery_edit, editDescription(title), MaterialTheme.colorScheme.primary, it)
        }
        onDelete?.let {
            ActionButton(R.string.theme_gallery_delete, deleteDescription(title), MaterialTheme.colorScheme.error, it)
        }
        if (selected) SelectedBadge()
    }
}

@Composable
private fun ActionButton(
    labelRes: Int,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(labelRes),
            color = color,
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
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
