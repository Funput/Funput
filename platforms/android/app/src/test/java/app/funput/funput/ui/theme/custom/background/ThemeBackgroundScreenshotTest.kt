package app.funput.funput.ui.theme.custom.background

import androidx.compose.ui.test.junit4.createComposeRule
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

/** The background image screen before an image is chosen and with one, in every variant. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class ThemeBackgroundScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun empty() = compose.captureScreen("theme-background-empty", variant, root = APP_SCREENSHOT_ROOT) {
        BackgroundTestHost(isDark = variant.isDark)
    }

    @Test
    fun image() {
        val path = testBackgroundImage()
        compose.captureScreen("theme-background-image", variant, root = APP_SCREENSHOT_ROOT) {
            BackgroundTestHost(isDark = variant.isDark, imagePath = path)
        }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
