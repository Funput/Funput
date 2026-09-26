package app.funput.funput.uitesting

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.RuntimeEnvironment

/** Where captures land unless a caller names another root, relative to the module directory. */
const val DEFAULT_SCREENSHOT_ROOT: String = "build/outputs/roborazzi"

/**
 * Renders [content] under [variant] and captures it as `<root>/<screen>/<variant>.png`.
 *
 * The system night mode and font scale are applied before composition, so both the app theme and
 * anything that reads the configuration directly see the same condition. Whether the capture is
 * written, compared or skipped is Roborazzi's call from the Gradle task that ran the test:
 * `recordRoborazziDebug` writes, `verifyRoborazziDebug` compares, a plain unit-test run only
 * composes the screen, which still fails the test if it crashes.
 *
 * [prepare] runs after composition and before the capture, to put the screen in the state being
 * captured (scrolled, a sheet open, a field focused).
 */
fun ComposeContentTestRule.captureScreen(
    screen: String,
    variant: ScreenshotVariant,
    root: String = DEFAULT_SCREENSHOT_ROOT,
    prepare: ComposeContentTestRule.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    RuntimeEnvironment.setQualifiers(if (variant.isDark) "+night" else "+notnight")
    RuntimeEnvironment.setFontScale(variant.fontScale)
    setContent(content)
    waitForIdle()
    prepare()
    waitForIdle()
    onRoot().captureRoboImage("$root/$screen/${variant.fileName}.png")
}
