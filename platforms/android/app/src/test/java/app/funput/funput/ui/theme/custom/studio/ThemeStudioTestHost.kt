package app.funput.funput.ui.theme.custom.studio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.theme.custom.CreateCustomThemeScreen

/** The built-in themes the studio offers as starting points. */
internal val studioBaseThemes: List<KeyboardThemeDescriptor> by lazy { BuiltInKeyboardThemeSource.loadThemes() }

/** The studio as the app shows it, on solid bars so captures do not depend on blur. */
@Composable
internal fun ThemeStudioTestHost(
    isDark: Boolean = false,
    editingTheme: KeyboardThemeDescriptor? = null,
    onSave: (CustomThemeDraft) -> Unit = {},
) {
    FunputUiTheme(isDark = isDark) {
        CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
            CreateCustomThemeScreen(
                baseThemes = studioBaseThemes,
                editingTheme = editingTheme,
                onSave = onSave,
                onBack = {},
            )
        }
    }
}
