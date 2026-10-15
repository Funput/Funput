package app.funput.funput.ui.theme.custom.studio

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import app.funput.funput.theme.KeyboardLabelWeight
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The key label size and weight chosen on the keys page are what the saved theme carries (#499). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ThemeStudioLabelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `label size and weight chosen on the keys page are saved with the theme`() {
        var saved: CustomThemeDraft? = null
        compose.setContent { ThemeStudioTestHost(onSave = { saved = it }) }

        compose.onNodeWithText("Phím & chữ").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Đậm").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Cỡ chữ").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(1.2f) }
        compose.onNodeWithText("120%").assertExists()
        compose.onNodeWithText("Lưu chủ đề").performClick()

        compose.runOnIdle {
            assertEquals(KeyboardLabelWeight.BOLD, saved?.theme?.labelWeight)
            assertEquals(1.2f, saved?.theme?.labelScale ?: 0f, 0.001f)
        }
    }
}
