package app.funput.funput.ui.shortcuts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun ShortcutsLoadStatus(loading: Boolean, error: Boolean, retry: () -> Unit) {
    if (!loading && !error) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = Modifier.fillMaxWidth().padding(Spacing.Section),
    ) {
        if (loading) CircularProgressIndicator()
        Text(stringResource(if (loading) R.string.shortcuts_loading else R.string.shortcuts_load_error),
            style = MaterialTheme.typography.bodyLarge)
        if (error) Button(onClick = retry) { Text(stringResource(R.string.shortcuts_retry)) }
    }
}

@Composable
internal fun ShortcutsEmptyState(title: String, body: String, action: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.let { TextButton(onClick = it) { Text(stringResource(R.string.shortcuts_add)) } }
    }
}
