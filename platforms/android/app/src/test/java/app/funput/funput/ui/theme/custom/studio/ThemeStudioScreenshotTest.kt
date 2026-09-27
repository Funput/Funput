package app.funput.funput.ui.theme.custom.studio

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ui.settings.APP_SCREENSHOT_ROOT
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import app.funput.funput.uitesting.ScreenshotVariant
import app.funput.funput.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The theme studio's first page and its keys page, in every appearance and font scale. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ThemeStudioScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun general() = capture("theme-studio-general")

    @Test
    fun keys() = capture("theme-studio-keys") { onNodeWithText("Phím & chữ").performClick() }

    private fun capture(screen: String, prepare: ComposeContentTestRule.() -> Unit = {}) =
        compose.captureScreen(screen, variant, root = APP_SCREENSHOT_ROOT, prepare = prepare) {
            ThemeStudioTestHost(isDark = variant.isDark)
        }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
