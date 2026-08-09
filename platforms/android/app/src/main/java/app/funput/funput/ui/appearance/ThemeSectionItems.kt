package app.funput.funput.ui.appearance

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.stringResource
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.ui.settings.components.SettingsSectionHeader
import app.funput.funput.ui.theme.gallery.ThemeCard

/**
 * A heading and one card per theme, emitted straight into the page's own list.
 *
 * These used to be rows that scrolled sideways inside the page. At the width a keyboard preview
 * needs, that showed a card and a bit of the next one, with no way to tell how many were behind it
 * — and it put a sideways scroll inside a vertical one.
 */
internal fun LazyListScope.themeSection(
    key: String,
    @StringRes titleRes: Int,
    themes: List<KeyboardThemeDescriptor>,
    state: AppearanceScreenState,
    onRequestDelete: (KeyboardThemeDescriptor) -> Unit,
    showsWhenEmpty: Boolean = false,
) {
    if (themes.isEmpty() && !showsWhenEmpty) return
    // The same header the two groups above use: one screen, one way of naming a section.
    item(key = "$key-title") { SettingsSectionHeader(stringResource(titleRes)) }
    if (themes.isEmpty()) {
        item(key = "$key-empty") { ThemeEmptyState() }
        return
    }
    items(items = themes, key = { descriptor -> "$key-${descriptor.id.value}" }) { descriptor ->
        val isCustom = descriptor.origin == KeyboardThemeOrigin.CUSTOM
        ThemeCard(
            descriptor = descriptor,
            selected = descriptor.id == state.selectedThemeId,
            onSelected = { state.onThemeSelected(descriptor.id) },
            onEdit = if (isCustom) {
                { state.onEditTheme(descriptor.id) }
            } else {
                null
            },
            onDelete = if (isCustom) {
                { onRequestDelete(descriptor) }
            } else {
                null
            },
        )
    }
}
