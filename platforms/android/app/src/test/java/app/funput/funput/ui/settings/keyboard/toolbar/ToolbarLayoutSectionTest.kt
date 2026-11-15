package app.funput.funput.ui.settings.keyboard.toolbar

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.settings.layout.ToolbarLayoutPreferences
import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class ToolbarLayoutSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `moving a slot down updates preferences`() {
        val updates = mutableListOf<ToolbarLayoutPreferences>()
        var current = ToolbarLayoutPreferences(
            leading = listOf(ToolbarSlot.MICROPHONE, ToolbarSlot.PLACEMENT),
            trailing = listOf(ToolbarSlot.EMOJI),
        )
        compose.setContent {
            FunputUiTheme(isDark = false) {
                ToolbarLayoutSection(
                    ToolbarLayoutSectionState(
                        preferences = current,
                        onMoveUp = { side, index ->
                            current = ToolbarLayoutEdits.moveUp(current, side, index)
                            updates += current
                        },
                        onMoveDown = { side, index ->
                            current = ToolbarLayoutEdits.moveDown(current, side, index)
                            updates += current
                        },
                        onMoveToOtherSide = { side, index ->
                            current = ToolbarLayoutEdits.moveToOtherSide(current, side, index)
                            updates += current
                        },
                    ),
                )
            }
        }
        compose.onAllNodes(hasText("Xuống")).get(0).performClick()
        assertEquals(listOf(ToolbarSlot.PLACEMENT, ToolbarSlot.MICROPHONE), updates.last().leading)
    }
}
