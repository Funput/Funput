package app.funput.funput.ui.settings.clipboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Rule
import org.junit.Test

class ClipboardSettingsThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun narrowLightThemeKeepsEveryActionVisible() = render(AppearanceMode.LIGHT)

    @Test
    fun narrowDarkThemeKeepsEveryActionVisible() = render(AppearanceMode.DARK)

    @Test
    fun narrowSystemThemeKeepsEveryActionVisible() = render(AppearanceMode.SYSTEM)

    @Test
    fun narrowDynamicThemeKeepsEveryActionVisible() = render(AppearanceMode.SYSTEM, true)

    private fun render(appearance: AppearanceMode, dynamicColor: Boolean = false) {
        compose.setContent {
            FunputTheme(appearanceMode = appearance, dynamicColor = dynamicColor) {
                Box(modifier = Modifier.width(320.dp)) {
                    ClipboardSettingsSection(
                        enabled = true,
                        expiry = ClipboardExpiry.WEEK,
                        onEnabledChanged = {},
                        onOpenExpiry = {},
                        onClear = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Lưu lịch sử bảng nhớ tạm").assertIsDisplayed()
        compose.onNodeWithText("1 tuần").assertIsDisplayed()
        compose.onNodeWithText("Xoá tất cả").assertIsDisplayed()
    }
}
