package app.funput.funput.ui.about

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ui.theme.FunputTheme

@Preview(name = "Giới thiệu · Sáng", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AboutLightPreview() {
    FunputTheme(appearanceMode = AppearanceMode.LIGHT) {
        AboutScreen(versionName = "1.2026.53", onOpenLink = {})
    }
}

@Preview(
    name = "Giới thiệu · Tối",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AboutDarkPreview() {
    FunputTheme(appearanceMode = AppearanceMode.DARK) {
        AboutScreen(versionName = "1.2026.53", onOpenLink = {})
    }
}
