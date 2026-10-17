package app.funput.funput.ui.kit.tiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A toggle tile is one switch named by its long title, and a locked one says why and stays put. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class ToggleTileSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    private val isSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    @Test
    fun `a tile is a switch that flips`() {
        compose.setContent {
            var on by remember { mutableStateOf(false) }
            FunputUiTheme(isDark = false) {
                FunputToggleTile(
                    title = "Rung",
                    state = if (on) "Bật" else "Tắt",
                    icon = FunputIcons.Haptics,
                    checked = on,
                    onCheckedChange = { on = it },
                    accessibilityTitle = "Rung khi nhấn phím",
                )
            }
        }
        val tile = compose.onNodeWithContentDescription("Rung khi nhấn phím")

        tile.assert(isSwitch).assertIsOff().performClick()
        tile.assertIsOn().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Bật"))
    }

    @Test
    fun `a locked tile is disabled and gives its reason`() {
        var changes = 0
        compose.setContent {
            FunputUiTheme(isDark = false) {
                FunputToggleTile(
                    title = "Hàng số",
                    state = "Luôn bật",
                    icon = FunputIcons.NumberRow,
                    checked = true,
                    onCheckedChange = { changes += 1 },
                    locked = true,
                    lockedReason = "VNI dùng hàng số để bỏ dấu",
                )
            }
        }
        val tile = compose.onNodeWithContentDescription("Hàng số")

        tile.assertIsOn().assertIsNotEnabled()
        tile.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Luôn bật. VNI dùng hàng số để bỏ dấu"),
        )
        tile.performClick()
        assertEquals(0, changes)
    }
}
