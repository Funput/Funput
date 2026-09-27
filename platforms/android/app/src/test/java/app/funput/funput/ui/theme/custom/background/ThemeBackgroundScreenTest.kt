package app.funput.funput.ui.theme.custom.background

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.funput.funput.ui.theme.custom.draft.ThemeDraftState
import app.funput.funput.ui.theme.custom.studio.ThemeStudioTestHost
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The background image screen: choosing, removing, and getting back to the studio. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ThemeBackgroundScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `without an image the screen only offers to choose one`() {
        var chooses = 0
        compose.setContent { BackgroundTestHost(onChooseImage = { chooses += 1 }) }

        compose.onNodeWithText("Phần ảnh hiển thị", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("Chọn ảnh").performClick()
        compose.runOnIdle { assertEquals(1, chooses) }
    }

    @Test
    fun `with an image it frames, blends, and can be removed`() {
        var state: ThemeDraftState? = null
        val path = testBackgroundImage()
        compose.setContent { BackgroundTestHost(imagePath = path, onState = { state = it }) }

        compose.onNodeWithText("Phần ảnh hiển thị", ignoreCase = true).assertExists()
        compose.onNodeWithText("Độ hiện ảnh nền").assertExists()
        compose.onNodeWithText("Gỡ ảnh").performScrollTo().performClick()
        compose.runOnIdle { assertNull(state?.backgroundImage) }
        compose.onNodeWithText("Chọn ảnh").assertExists()
    }

    @Test
    fun `system back returns from the image editor to the studio`() {
        compose.setContent { ThemeStudioTestHost() }
        compose.onNodeWithText("Nền").performClick()
        compose.onNodeWithText("Ảnh nền").performClick()
        compose.onNodeWithText("Chọn ảnh").assertExists()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Lưu chủ đề").assertExists()
        compose.onNodeWithText("Chọn ảnh").assertDoesNotExist()
    }
}
