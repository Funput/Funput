package app.funput.funput.ui.settings.groups

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.smart.SmartSection
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each of the seven smart switches flips its own preference, and the suggestion card leads into the dictionary. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SmartSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `all seven preferences dispatch from the smart page`() {
        val changes = mutableMapOf<String, Boolean>()
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SmartSection(testSettingsState(onSmartChanged = { key, value -> changes[key] = value }))
            }
        }

        // A semantics click, not a touch: seven rows run past the test screen's bottom edge.
        listOf(
            "Tự khôi phục từ tiếng Anh", "Kiểm tra chính tả", "Tự viết hoa", "Tự sửa lỗi gõ nhầm phím",
            "Gợi ý từ", "Cử chỉ thông minh", "Tự về bảng chữ",
        ).forEach { compose.onNodeWithText(it).performSemanticsAction(SemanticsActions.OnClick) }

        assertEquals(
            mapOf(
                "restore" to false,
                "spelling" to true,
                "capitalization" to false,
                "typos" to false,
                "suggestions" to false,
                "gestures" to false,
                "letters" to false,
            ),
            changes,
        )
    }

    @Test
    fun `the suggestion card leads into the personal dictionary`() {
        var opened = 0
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SmartSection(testSettingsState(onSmartChanged = { _, _ -> }), onOpenDictionary = { opened++ })
            }
        }

        compose.onNodeWithText("Từ điển cá nhân").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, opened)
    }
}
