package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R

@Composable
internal fun ThemeInfoTab(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomThemeSection(title = stringResource(R.string.custom_theme_info_title), modifier = modifier) {
        Column {
            Text(
                text = stringResource(R.string.custom_theme_info_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.custom_theme_name_label)) },
                placeholder = { Text(stringResource(R.string.custom_theme_name_placeholder)) },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .testTag("custom-theme-name"),
            )
        }
    }
}
