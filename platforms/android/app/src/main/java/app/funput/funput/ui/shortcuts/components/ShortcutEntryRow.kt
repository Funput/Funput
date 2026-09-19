package app.funput.funput.ui.shortcuts.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = enabled, onClick = onEdit)
                .padding(start = Spacing.Large, top = Spacing.Medium,
                    bottom = Spacing.Medium, end = Spacing.Large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Tight),
                modifier = Modifier.weight(1f)) {
                Text(entry.trigger, style = MaterialTheme.typography.bodyLarge)
                Text(entry.expansion, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
            Icon(painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.shortcuts_delete_action, entry.trigger),
                tint = MaterialTheme.colorScheme.error)
        }
        }
    }
}
