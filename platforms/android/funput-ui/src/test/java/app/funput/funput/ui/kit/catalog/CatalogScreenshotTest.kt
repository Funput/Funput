package app.funput.funput.ui.kit.catalog

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ui.kit.KIT_SCREENSHOT_ROOT
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
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
 * The catalog's three tabs as they open on a device. The catalog is also the review surface for
 * P1, so a component that breaks in context (not just alone) shows up here.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = ScreenshotDevices.PHONE)
class CatalogScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun foundation() = capture("catalog-foundation", tab = null)

    @Test
    fun rows() = capture("catalog-rows", tab = "Hàng")

    @Test
    fun controls() = capture("catalog-controls", tab = "Điều khiển")

    private fun capture(screen: String, tab: String?) = compose.captureScreen(
        screen = screen,
        variant = variant,
        root = KIT_SCREENSHOT_ROOT,
        prepare = { tab?.let { onNodeWithText(it).performClick() } },
    ) {
        // The catalog's own "follow the device" default would pick GLASS on API 35; JVM
        // rendering cannot draw it, so the solid tier is pinned from outside.
        CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) { FunputCatalog(isDark = variant.isDark) }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}
