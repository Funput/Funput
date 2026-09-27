package app.funput.funput.ui.kit.rows

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each row is one accessible item with the right role, state and touch size. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class RowSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun `the whole toggle row is one switch that flips its state`() {
        compose.setContent {
            var on by remember { mutableStateOf(false) }
            FunputUiTheme(isDark = false) {
                ToggleRow(
                    title = "Rung khi gõ",
                    summary = "Phản hồi nhẹ",
                    checked = on,
                    onCheckedChange = { on = it },
                )
            }
        }
        val row = compose.onNodeWithText("Rung khi gõ")

        row.assert(role(Role.Switch)).assertIsOff().assertHeightIsAtLeast(48.dp)
        row.performClick()
        row.assertIsOn()
    }

    @Test
    fun `a link row is a button that reports taps`() {
        var taps = 0
        compose.setContent {
            FunputUiTheme(isDark = false) { LinkRow(title = "Gõ tắt", value = "12", onClick = { taps++ }) }
        }

        compose.onNodeWithText("Gõ tắt").assert(role(Role.Button)).performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `a choice row is a radio button carrying its selection`() {
        compose.setContent {
            FunputUiTheme(isDark = false) { ChoiceRow(title = "VNI", selected = true, onSelect = {}) }
        }

        compose.onNodeWithText("VNI").assert(role(Role.RadioButton)).assertIsSelected()
    }
}
