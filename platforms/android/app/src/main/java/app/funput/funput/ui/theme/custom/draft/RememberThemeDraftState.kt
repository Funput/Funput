package app.funput.funput.ui.theme.custom.draft

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import app.funput.funput.theme.KeyboardThemeDescriptor

@Composable
internal fun rememberThemeDraftState(
    baseThemes: List<KeyboardThemeDescriptor>,
    editingTheme: KeyboardThemeDescriptor?,
): ThemeDraftState = rememberSaveable(
    editingTheme?.id?.value,
    saver = ThemeDraftState.saver(baseThemes, editingTheme),
    // rememberSaveable restores by key without checking its inputs, so the key itself names the
    // theme: a draft saved while editing one theme can then never come back inside another.
    key = "theme-draft:${editingTheme?.id?.value ?: NewThemeDraftKey}",
) {
    ThemeDraftState(baseThemes, editingTheme)
}

private const val NewThemeDraftKey = "new"
