package app.funput.funput.ui.settings.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.hub.previewSettingsState

@Preview(name = "Page, typing", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TypingPagePreview() {
    PagePreview(SettingsPage.TYPING)
}

@Preview(name = "Page, layout", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LayoutPagePreview() {
    PagePreview(SettingsPage.LAYOUT)
}

@Preview(name = "Page, data", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DataPagePreview() {
    PagePreview(SettingsPage.DATA)
}

@Composable
private fun PagePreview(page: SettingsPage) {
    FunputUiTheme(isDark = false) {
        SettingsPageScreen(page = page, state = previewSettingsState(), onBack = {})
    }
}
