package app.funput.funput.ui.theme.custom

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.ui.theme.FunputTheme

@Preview(name = "Create custom theme · Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CreateCustomThemeLightPreview() {
    FunputTheme {
        CreateCustomThemeScreen(
            baseThemes = BuiltInKeyboardThemeSource.loadThemes(),
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(
    name = "Create custom theme · Dark",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CreateCustomThemeDarkPreview() {
    FunputTheme(appearanceMode = AppearanceMode.DARK) {
        CreateCustomThemeScreen(
            baseThemes = BuiltInKeyboardThemeSource.loadThemes(),
            onSave = {},
            onBack = {},
        )
    }
}
