package app.funput.funput.ui.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
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

/** Where the app's goldens live; see funput-ui for how they are recorded and verified. */
internal const val APP_SCREENSHOT_ROOT: String = "src/test/screenshots"

/**
 * The rebuilt Settings page: first launch (setup card under the keyboard preview) and a ready
 * keyboard scrolled to the layout group, in every appearance and font scale, in Vietnamese.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class SettingsScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun setup() = capture("settings-setup", KeyboardSetupStatus.NOT_ENABLED)

    @Test
    fun layout() = capture("settings-layout", KeyboardSetupStatus.READY) {
        onNodeWithTag(FunputScreenListTag).performScrollToNode(hasText("BỐ CỤC BÀN PHÍM"))
    }

    private fun capture(
        screen: String,
        status: KeyboardSetupStatus,
        prepare: ComposeContentTestRule.() -> Unit = {},
    ) = compose.captureScreen(screen, variant, root = APP_SCREENSHOT_ROOT, prepare = prepare) {
        FunputUiTheme(isDark = variant.isDark) {
            CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
                SettingsScreen(testSettingsState(status))
            }
        }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
