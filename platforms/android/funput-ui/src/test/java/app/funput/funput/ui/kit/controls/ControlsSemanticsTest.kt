package app.funput.funput.ui.kit.controls

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Roles, states and touch targets of the standalone controls and the dialog. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class ControlsSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun `segments are radio buttons in one group and report the tapped index`() {
        var picked = -1
        compose.setContent {
            FunputUiTheme(isDark = false) {
                FunputSegmented(
                    options = listOf("Truyền thống", "Hiện đại"),
                    selectedIndex = 0,
                    onSelect = { picked = it },
                )
            }
        }

        compose.onNodeWithText("Truyền thống").assert(role(Role.RadioButton)).assertIsSelected()
        compose.onNodeWithText("Hiện đại").assertIsNotSelected().assertHeightIsAtLeast(42.dp).performClick()

        assertEquals(1, picked)
    }

    @Test
    fun `a disabled button is announced as disabled and ignores taps`() {
        var taps = 0
        compose.setContent {
            FunputUiTheme(isDark = false) { FunputButton("Lưu", onClick = { taps++ }, enabled = false) }
        }

        compose.onNodeWithText("Lưu").assert(role(Role.Button)).assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Lưu").performClick()

        assertEquals(0, taps)
    }

    @Test
    fun `a toggle inside a row has no semantics of its own`() {
        compose.setContent { FunputUiTheme(isDark = false) { FunputToggle(checked = true, onCheckedChange = null) } }

        assertEquals(0, compose.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
    }

    @Test
    fun `a destructive dialog confirms through its own button`() {
        var confirmed = false
        compose.setContent {
            FunputUiTheme(isDark = false) {
                FunputDialog(
                    title = "Xoá từ đã học?",
                    message = "Không thể hoàn tác.",
                    confirmLabel = "Xoá",
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                    dismissLabel = "Huỷ",
                    destructive = true,
                )
            }
        }

        compose.onNodeWithText("Không thể hoàn tác.").assertExists()
        compose.onNodeWithText("Xoá").performClick()

        assertEquals(true, confirmed)
    }
}
