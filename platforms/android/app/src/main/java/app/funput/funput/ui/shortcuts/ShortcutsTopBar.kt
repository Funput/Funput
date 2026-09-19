package app.funput.funput.ui.shortcuts

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.funput.funput.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortcutsTopBar(
    canWrite: Boolean,
    canShowOptions: Boolean,
    onBack: () -> Unit,
    onOptions: () -> Unit,
    onAdd: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.shortcuts_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.shortcuts_back))
            }
        },
        actions = {
            IconButton(onClick = onOptions, enabled = canShowOptions) {
                Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.shortcuts_options))
            }
            IconButton(onClick = onAdd, enabled = canWrite) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.shortcuts_add))
            }
        },
    )
}
