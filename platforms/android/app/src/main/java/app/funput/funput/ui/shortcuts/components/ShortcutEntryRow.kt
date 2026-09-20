package app.funput.funput.ui.shortcuts.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun ShortcutEntryRow(
    entry: TextShortcut,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxWidth().padding(Spacing.Large)) {
                Text(stringResource(R.string.shortcuts_swipe_delete),
                    color = MaterialTheme.colorScheme.error)
            }
        },
        modifier = Modifier.testTag("shortcuts-entry-${entry.id}"),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
                modifier = Modifier.clickable(enabled = enabled, onClick = onEdit)
                    .heightIn(min = 48.dp)
                    .padding(Spacing.Large, Spacing.Medium),
            ) {
                Text(entry.trigger, style = MaterialTheme.typography.bodyLarge)
                Text(entry.expansion, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
