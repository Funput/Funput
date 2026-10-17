package app.funput.funput.ui.settings.hub

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The keyboard hub: preview, setup while unfinished, and one row per page, in Vietnamese. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class KeyboardHubScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<SettingsPage>()

    private fun show(state: SettingsScreenState) = compose.setContent {
        FunputUiTheme(isDark = false) { KeyboardHubScreen(state, onOpenPage = { opened += it }) }
    }

    private fun scrollTo(text: String) =
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText(text))

    @Test
    fun `a ready keyboard shows the preview and every page, and no setup card`() {
        show(testSettingsState(KeyboardSetupStatus.READY))

        // The large title and the top bar both carry it.
        compose.onAllNodesWithText("Bàn phím").onFirst().assertExists()
        compose.onNodeWithTag(SettingsHeroTag).assertExists()
        compose.onNodeWithTag(SettingsSetupTag).assertDoesNotExist()
        scrollTo("TẤT CẢ CÀI ĐẶT")
        val titles = listOf(
            "Gõ tiếng Việt", "Bố cục và kích thước", "Thông minh",
            "Phản hồi khi chạm", "Clipboard", "Bàn phím rời",
        )
        titles.forEach { title ->
            scrollTo(title)
            compose.onNodeWithText(title).assertExists()
        }
        // Data lives under "Tôi", and voice input only where the build ships it.
        compose.onNodeWithText("Dữ liệu").assertDoesNotExist()
        compose.onNodeWithText("Nhập bằng giọng nói").assertDoesNotExist()
    }

    @Test
    fun `each row says what its page is set to`() {
        show(testSettingsState(hapticsEnabled = true, soundsEnabled = true))

        scrollTo("Đặt dấu kiểu cũ")
        compose.onNodeWithText("Đặt dấu kiểu cũ").assertExists()
        scrollTo("Đang bật rung trung bình và âm thanh")
        compose.onNodeWithText("Đang bật rung trung bình và âm thanh").assertExists()
    }

    @Test
    fun `voice input gets its row where the build ships it`() {
        show(testSettingsState(speechAvailable = true))

        scrollTo("Nhập bằng giọng nói")
        compose.onNodeWithText("Nhập bằng giọng nói").assertExists()
    }

    @Test
    fun `unfinished setup asks for the next step`() {
        show(testSettingsState(KeyboardSetupStatus.NOT_SELECTED))

        compose.onNodeWithTag(SettingsSetupTag).assertExists()
        compose.onNodeWithText("Bước 2 trên 2").assertExists()
        compose.onNodeWithText("Chọn bàn phím").assertExists()
    }

    @Test
    fun `a row opens its page`() {
        show(testSettingsState())

        scrollTo("Phản hồi khi chạm")
        compose.onNodeWithText("Phản hồi khi chạm").performClick()

        compose.runOnIdle { assertEquals(listOf(SettingsPage.FEEDBACK), opened) }
    }
}
