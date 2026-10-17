package app.funput.funput.ui.me

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ui.kit.theme.FunputUiTheme

@Preview(name = "Tôi, sáng", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MeLightPreview() {
    FunputUiTheme(isDark = false) {
        MeScreen(versionName = "1.2026.53", onOpenLink = {})
    }
}

@Preview(
    name = "Tôi, tối",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MeDarkPreview() {
    FunputUiTheme(isDark = true) {
        MeScreen(versionName = "1.2026.53", onOpenLink = {})
    }
}
