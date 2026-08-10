package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.funput.funput.R

/**
 * Title bar carrying the theme name and the restore action.
 *
 * The name is edited here rather than on a tab of its own: it is a label for the thing, not a
 * design decision, and putting it behind a tab meant the disabled save button had to explain
 * where to go and come back from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeStudioTopBar(
    state: ThemeDraftState,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            TextField(
                value = state.name,
                onValueChange = { state.name = it },
                placeholder = { Text(stringResource(R.string.custom_theme_name_hint)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom-theme-name"),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.custom_theme_back_description),
                )
            }
        },
        // Restoring means "undo what I changed", and nothing else. Choosing what the theme is
        // built on is a decision of its own, and it is now the first thing on the screen.
        actions = {
            TextButton(onClick = state::restoreBase) {
                Text(stringResource(R.string.custom_theme_restore))
            }
        },
    )
}
