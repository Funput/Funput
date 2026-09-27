package app.funput.funput.ui.kit.rows

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LongValue = "Telex nâng cao (gõ w thành ư, bỏ dấu kiểu mới)"

/**
 * Where a row's value ends up: beside the title when it fits, under it when it does not.
 * Native graphics are required: the legacy mode fakes text measurement, so every label would
 * look a few dp wide and always fit.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK])
class RowLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    // Text is read from the unmerged tree: a clickable row merges its children into one node,
    // and these tests are about where each child is drawn.

    private fun show(value: String, fontScale: Float = 1f) {
        RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            FunputUiTheme(isDark = false) {
                LinkRow(title = "Kiểu gõ", value = value, onClick = {}, modifier = Modifier.width(360.dp))
            }
        }
    }

    @Test
    fun `a short value sits beside the title`() {
        show("VNI")

        val title = compose.onNodeWithText("Kiểu gõ", useUnmergedTree = true).getBoundsInRoot()
        val value = compose.onNodeWithText("VNI", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("value should be to the right of the title", value.left > title.right)
        assertTrue("value should share the title's line", value.top < title.bottom)
    }

    @Test
    fun `a long value moves under the title instead of squeezing it`() {
        show(LongValue, fontScale = 1.3f)

        val title = compose.onNodeWithText("Kiểu gõ", useUnmergedTree = true).getBoundsInRoot()
        val value = compose.onNodeWithText(LongValue, useUnmergedTree = true).getBoundsInRoot()
        assertTrue("value should start below the title", value.top >= title.bottom)
        assertTrue("value should line up with the title", value.left == title.left)
    }
}
