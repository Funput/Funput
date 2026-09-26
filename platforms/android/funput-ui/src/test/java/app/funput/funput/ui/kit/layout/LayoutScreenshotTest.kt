package app.funput.funput.ui.kit.layout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import app.funput.funput.ui.kit.KIT_SCREENSHOT_ROOT
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

/**
 * The screen frame at rest (large title, bare top bar) and scrolled (compact title on a solid
 * bar, content passing under it and under the tab bar), in every appearance and font scale.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = ScreenshotDevices.PHONE)
class LayoutScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun atTop() = compose.captureScreen("screen-top", variant, root = KIT_SCREENSHOT_ROOT) {
        SampleScreen(isDark = variant.isDark, onBack = {})
    }

    @Test
    fun scrolled() = compose.captureScreen(
        screen = "screen-scrolled",
        variant = variant,
        root = KIT_SCREENSHOT_ROOT,
        prepare = { onNodeWithTag(FunputScreenListTag).performScrollToIndex(2) },
    ) {
        SampleScreen(isDark = variant.isDark, onBack = {})
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
