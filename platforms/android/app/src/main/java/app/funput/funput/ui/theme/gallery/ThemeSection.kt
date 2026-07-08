package app.funput.funput.ui.theme.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin

@Composable
internal fun ThemeSection(
    title: String,
    themes: List<KeyboardThemeDescriptor>,
    selectedThemeId: KeyboardThemeId,
    onThemeSelected: (KeyboardThemeId) -> Unit,
    onEditTheme: (KeyboardThemeId) -> Unit,
    onDeleteTheme: (KeyboardThemeDescriptor) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = title,
    leadingContent: @Composable ((Modifier) -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 20.dp),
            modifier = Modifier.testTag(testTag),
        ) {
            leadingContent?.let { content ->
                item(key = "$testTag-leading") {
                    content(Modifier.width(ThemeCardWidth))
                }
            }
            items(
                items = themes,
                key = { descriptor -> descriptor.id.value },
            ) { descriptor ->
                val isCustom = descriptor.origin == KeyboardThemeOrigin.CUSTOM
                ThemeCard(
                    descriptor = descriptor,
                    selected = descriptor.id == selectedThemeId,
                    onSelected = { onThemeSelected(descriptor.id) },
                    onEdit = if (isCustom) {
                        { onEditTheme(descriptor.id) }
                    } else {
                        null
                    },
                    onDelete = if (isCustom) {
                        { onDeleteTheme(descriptor) }
                    } else {
                        null
                    },
                    modifier = Modifier.width(ThemeCardWidth),
                )
            }
        }
    }
}

internal val ThemeCardWidth = 312.dp
