package app.funput.funput.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.typing.TypingSettingsSectionTag
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The settings page's structure and the typing group's actions, in Vietnamese as users see them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: SettingsScreenState) = compose.setContent {
        FunputUiTheme(isDark = false) { SettingsScreen(state) }
    }

    private fun scrollTo(text: String) =
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText(text))

    @Test
    fun `a ready keyboard shows the preview and every group, and no setup card`() {
        show(testSettingsState(KeyboardSetupStatus.READY))

        compose.onNodeWithTag(SettingsHeroTag).assertExists()
        compose.onNodeWithTag(SettingsSetupTag).assertDoesNotExist()
        val headers = listOf(
            "GÕ TIẾNG VIỆT", "BỐ CỤC BÀN PHÍM", "THÔNG MINH", "PHỤ ÂM ĐẦU MỞ RỘNG",
            "PHẢN HỒI KHI CHẠM", "CLIPBOARD", "DỮ LIỆU",
        )
        headers.forEach { header ->
            scrollTo(header)
            compose.onNodeWithText(header).assertExists()
        }
    }

    @Test
    fun `unfinished setup asks for the next step`() {
        show(testSettingsState(KeyboardSetupStatus.NOT_SELECTED))

        compose.onNodeWithTag(SettingsSetupTag).assertExists()
        compose.onNodeWithText("Bước 2 trên 2").assertExists()
        compose.onNodeWithText("Chọn bàn phím").assertExists()
    }

    @Test
    fun `the typing group opens the picker, sets the tone style and opens shortcuts`() {
        var tone: ToneStyle? = null
        var openedShortcuts = false
        show(testSettingsState(onToneStyleSelected = { tone = it }, onOpenShortcuts = { openedShortcuts = true }))
        val inTyping = hasAnyAncestor(hasTestTag(TypingSettingsSectionTag))

        compose.onNode(hasText("Hiện đại") and inTyping).performClick()
        compose.onNode(hasText("Gõ tắt") and inTyping).performClick()
        compose.onNodeWithText("Kiểu gõ").performClick()

        assertEquals(ToneStyle.MODERN, tone)
        assertTrue(openedShortcuts)
        // The input method row opens its sheet, titled like the row.
        compose.onNodeWithText("Dùng các phím số để nhập dấu.").assertIsDisplayed()
    }
}
