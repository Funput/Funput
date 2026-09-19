package app.funput.funput.ui.settings.smart

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SmartSettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allFivePreferencesDispatchFromOneSection() {
        val changes = mutableMapOf<String, Boolean>()
        compose.setContent {
            FunputTheme {
                SmartSettingsSection(
                    preferences = SmartCompositionPreferences.Default,
                    personalSuggestionsEnabled = true,
                    smartGesturesEnabled = true,
                    onSmartRestoreChanged = { changes["restore"] = it },
                    onSpellCheckChanged = { changes["spelling"] = it },
                    onAutoCapitalizeChanged = { changes["capitalization"] = it },
                    onPersonalSuggestionsChanged = { changes["suggestions"] = it },
                    onSmartGesturesChanged = { changes["gestures"] = it },
                )
            }
        }

        compose.onNodeWithText("Tự khôi phục từ tiếng Anh").performClick()
        compose.onNodeWithText("Kiểm tra chính tả").performClick()
        compose.onNodeWithText("Tự viết hoa").performClick()
        compose.onNodeWithText("Gợi ý từ").performClick()
        compose.onNodeWithText("Cử chỉ thông minh").performClick()

        compose.runOnIdle {
            assertEquals(
                mapOf(
                    "restore" to false,
                    "spelling" to true,
                    "capitalization" to false,
                    "suggestions" to false,
                    "gestures" to false,
                ),
                changes,
            )
        }
    }
}
