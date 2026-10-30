package app.funput.funput.ui.settings.enterkey

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.waitForText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Choosing and removing the apps whose Enter key sends. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class EnterKeyScreenTest {
    @get:Rule
    val compose = createComposeRule()
    private val added = mutableListOf<String>()
    private val removed = mutableListOf<String>()
    private val installed = listOf(
        InstalledApp("com.facebook.orca", "Messenger"),
        InstalledApp("com.zing.zalo", "Zalo"),
    )

    @Test fun `with nothing chosen the page explains itself and suggests messenger`() {
        show(emptySet())

        compose.onNodeWithText("Chưa chọn ứng dụng nào").assertExists()
        compose.onNodeWithText("Thêm").performClick()
        assertEquals(listOf("com.facebook.orca"), added)
    }

    @Test fun `apps are added from the picker, found by search`() {
        show(emptySet())

        compose.onNodeWithContentDescription("Thêm ứng dụng").performClick()
        compose.onNodeWithText("Tìm ứng dụng").performTextInput("zal")
        compose.waitForText("Zalo")
        compose.onNodeWithTag("enter-key-pick-com.zing.zalo").performClick()

        assertEquals(listOf("com.zing.zalo"), added)
    }

    @Test fun `removing an app asks first`() {
        show(setOf("com.facebook.orca"))

        compose.onNodeWithTag("enter-key-app-com.facebook.orca").performTouchInput { swipeLeft() }
        compose.waitForText("Bỏ Messenger?")
        assertEquals(emptyList<String>(), removed)
        compose.onNodeWithText("Bỏ").performClick()

        assertEquals(listOf("com.facebook.orca"), removed)
    }

    private fun show(packages: Set<String>) {
        compose.setContent {
            FunputUiTheme(isDark = false) {
                EnterKeyScreen(EnterKeyScreenState(packages, installed, added::add, removed::add), onBack = {})
            }
        }
    }
}
