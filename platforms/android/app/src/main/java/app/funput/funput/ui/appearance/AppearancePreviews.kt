package app.funput.funput.ui.appearance

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.ui.theme.FunputTheme

@Preview(name = "Giao diện · Sáng", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppearanceLightPreview() {
    FunputTheme(appearanceMode = AppearanceMode.LIGHT) {
        AppearanceScreen(previewState(followsAppearance = false))
    }
}

@Preview(
    name = "Giao diện · Hai theme",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AppearanceSlotsPreview() {
    FunputTheme(appearanceMode = AppearanceMode.DARK) {
        AppearanceScreen(previewState(followsAppearance = true))
    }
}

private fun previewState(followsAppearance: Boolean): AppearanceScreenState {
    val themes = InstalledThemeRepository.builtIn().themes
    return AppearanceScreenState(
        appearanceMode = AppearanceMode.SYSTEM,
        dynamicColorEnabled = false,
        followsAppearance = followsAppearance,
        activeSlot = if (followsAppearance) KeyboardThemeSlot.LIGHT else KeyboardThemeSlot.SINGLE,
        lightThemeName = "Paper",
        darkThemeName = "Ink",
        systemThemes = themes.filter { it.origin == KeyboardThemeOrigin.BUILT_IN },
        userThemes = themes.filter { it.origin != KeyboardThemeOrigin.BUILT_IN },
        selectedThemeId = KeyboardThemeId.Dark,
        onAppearanceSelected = {},
        onDynamicColorChanged = {},
        onFollowsAppearanceChange = {},
        onSlotSelected = {},
        onThemeSelected = {},
        onCreateTheme = {},
        onEditTheme = {},
        onDeleteTheme = {},
    )
}
