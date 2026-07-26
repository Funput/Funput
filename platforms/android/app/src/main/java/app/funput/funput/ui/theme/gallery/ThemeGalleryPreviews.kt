package app.funput.funput.ui.theme.gallery

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.FunputTheme

@Preview(name = "Theme gallery · Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LightThemeGalleryPreview() {
    GalleryPreview(AppearanceMode.LIGHT)
}

@Preview(
    name = "Theme gallery · Dark",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DarkThemeGalleryPreview() {
    GalleryPreview(AppearanceMode.DARK)
}

@Composable
private fun GalleryPreview(appearanceMode: AppearanceMode) {
    val repository = InstalledThemeRepository.builtIn()

    FunputTheme(appearanceMode) {
        ThemeGalleryScreen(
            themes = repository.themes,
            selectedThemeId = KeyboardThemeId.Dark,
            followsAppearance = true,
            activeSlot = KeyboardThemeSlot.DARK,
            onThemeSelected = {},
            onFollowsAppearanceChange = {},
            onSlotSelected = {},
            onCreateTheme = {},
            onEditTheme = {},
            onDeleteTheme = {},
            onBack = {},
        )
    }
}
