package app.funput.funput.ui.settings

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.settings.data.DataSettingsSectionTag
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.smart.SmartSettingsSectionTag
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenGroupingTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun readyScreenUsesFocusedGroups() {
        setScreen(KeyboardSetupStatus.READY)

        compose.onNodeWithTag(SettingsSetupTag).assertDoesNotExist()
        list().performScrollToNode(hasText("GÕ TIẾNG VIỆT"))
        compose.onNodeWithText("GÕ TIẾNG VIỆT").assertExists()
        list().performScrollToNode(hasText("BỐ CỤC BÀN PHÍM"))
        compose.onNodeWithText("BỐ CỤC BÀN PHÍM").assertExists()
        list().performScrollToNode(hasText("THÔNG MINH"))
        compose.onNodeWithText("THÔNG MINH").assertExists()
        compose.onNode(
            hasText("Gợi ý từ") and hasAnyAncestor(hasTestTag(SmartSettingsSectionTag)),
        ).assertExists()
        compose.onNode(
            hasText("Cử chỉ thông minh") and hasAnyAncestor(hasTestTag(SmartSettingsSectionTag)),
        ).assertExists()
        list().performScrollToNode(hasText("PHẢN HỒI KHI CHẠM"))
        compose.onNodeWithText("PHẢN HỒI KHI CHẠM").assertExists()
        list().performScrollToNode(hasText("CLIPBOARD"))
        compose.onNodeWithText("CLIPBOARD").assertExists()
        list().performScrollToNode(hasText("DỮ LIỆU"))
        compose.onNodeWithText("DỮ LIỆU").assertExists()
        compose.onNode(
            hasText("Từ đã học") and hasAnyAncestor(hasTestTag(DataSettingsSectionTag)),
        ).assertExists()
    }

    @Test
    fun incompleteSetupIsRendered() {
        setScreen(KeyboardSetupStatus.NOT_ENABLED)

        compose.onNodeWithTag(SettingsHeroTag).assertExists()
        list().performScrollToNode(hasText("Thiết lập Funput"))
        compose.onNodeWithTag(SettingsSetupTag).assertExists()
    }

    private fun setScreen(status: KeyboardSetupStatus) {
        compose.setContent {
            FunputTheme { SettingsScreen(testSettingsState(status)) }
        }
    }

    private fun list() = compose.onNodeWithTag(SettingsListTag)
}
