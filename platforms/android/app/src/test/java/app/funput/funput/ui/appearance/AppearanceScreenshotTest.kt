package app.funput.funput.ui.appearance

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
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

/**
 * The appearance tab: its top (app mode, keyboard mode with both slots), and the gallery scrolled
 * to the theme in use and to a theme the user made, in every appearance and font scale.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi-" + ScreenshotDevices.PHONE)
class AppearanceScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun top() = capture("appearance-top", testAppearanceState(followsAppearance = true, activeSlot = KeyboardThemeSlot.LIGHT))

    @Test
    fun gallery() = capture("appearance-gallery", testAppearanceState()) {
        onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(KeyboardThemeId.Dark.value))
    }

    @Test
    fun mine() = capture("appearance-mine", testAppearanceState(extraThemes = listOf(testCustomTheme))) {
        onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(testCustomTheme.id.value))
    }

    private fun capture(
        screen: String,
        state: AppearanceScreenState,
        prepare: ComposeContentTestRule.() -> Unit = {},
    ) = compose.captureScreen(screen, variant, root = APP_SCREENSHOT_ROOT, prepare = prepare) {
        FunputUiTheme(isDark = variant.isDark) {
            CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) { AppearanceScreen(state) }
        }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
