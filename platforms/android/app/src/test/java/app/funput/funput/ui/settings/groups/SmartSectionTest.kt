package app.funput.funput.ui.settings.groups

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.smart.SmartSection
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each of the six smart switches flips its own preference, and only its own. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SmartSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `all six preferences dispatch from one section`() {
        val changes = mutableMapOf<String, Boolean>()
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SmartSection(
                    preferences = SmartCompositionPreferences.Default,
                    personalSuggestionsEnabled = true,
                    smartGesturesEnabled = true,
                    returnsToLettersEnabled = true,
                    onSmartRestoreChanged = { changes["restore"] = it },
                    onSpellCheckChanged = { changes["spelling"] = it },
                    onAutoCapitalizeChanged = { changes["capitalization"] = it },
                    onPersonalSuggestionsChanged = { changes["suggestions"] = it },
                    onSmartGesturesChanged = { changes["gestures"] = it },
                    onReturnsToLettersChanged = { changes["letters"] = it },
                )
            }
        }

        listOf(
            "Tự khôi phục từ tiếng Anh", "Kiểm tra chính tả", "Tự viết hoa",
            "Gợi ý từ", "Cử chỉ thông minh", "Tự về bảng chữ",
        ).forEach { compose.onNodeWithText(it).performClick() }

        assertEquals(
            mapOf(
                "restore" to false,
                "spelling" to true,
                "capitalization" to false,
                "suggestions" to false,
                "gestures" to false,
                "letters" to false,
            ),
            changes,
        )
    }
}
