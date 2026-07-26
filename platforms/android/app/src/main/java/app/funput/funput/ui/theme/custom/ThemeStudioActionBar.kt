package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/**
 * Save and cancel, always reachable.
 *
 * Previously the save button was the last item of a scrolling list, so on the colors tab it sat
 * below eighteen rows and could not be seen while working.
 */
@Composable
internal fun ThemeStudioActionBar(
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                // Scaffold's contentWindowInsets covers the content slot, not this one, so the
                // buttons would otherwise sit under the system navigation bar.
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.custom_theme_cancel))
            }
            Button(
                enabled = canSave,
                onClick = onSave,
                modifier = Modifier.weight(2f),
            ) {
                Text(stringResource(R.string.custom_theme_save))
            }
        }
    }
}
