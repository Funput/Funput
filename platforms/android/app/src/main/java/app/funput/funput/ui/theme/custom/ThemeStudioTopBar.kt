package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor

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
    baseThemes: List<KeyboardThemeDescriptor>,
    onBack: () -> Unit,
) {
    var restoreOpen by remember { mutableStateOf(false) }

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
        actions = {
            TextButton(onClick = { restoreOpen = true }) {
                Text(stringResource(R.string.custom_theme_restore))
            }
            // Choosing a base and restoring one are the same operation — both replace every
            // token — so they are one control rather than two that would contradict each other.
            DropdownMenu(expanded = restoreOpen, onDismissRequest = { restoreOpen = false }) {
                baseThemes.forEach { descriptor ->
                    DropdownMenuItem(
                        text = { Text(descriptor.name) },
                        onClick = {
                            state.selectBaseTheme(descriptor.id.value)
                            restoreOpen = false
                        },
                    )
                }
            }
        },
    )
}
