package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

/** Stateless gallery for locally available keyboard themes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeGalleryScreen(
    themes: List<KeyboardThemeDescriptor>,
    selectedThemeId: KeyboardThemeId,
    onThemeSelected: (KeyboardThemeId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_gallery_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.theme_gallery_back_description),
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            item(key = "description") {
                Text(
                    text = stringResource(R.string.theme_gallery_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (themes.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.theme_gallery_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                items(
                    items = themes,
                    key = { descriptor -> descriptor.id.value },
                ) { descriptor ->
                    ThemeCard(
                        descriptor = descriptor,
                        selected = descriptor.id == selectedThemeId,
                        onSelected = { onThemeSelected(descriptor.id) },
                    )
                }
            }
        }
    }
}
