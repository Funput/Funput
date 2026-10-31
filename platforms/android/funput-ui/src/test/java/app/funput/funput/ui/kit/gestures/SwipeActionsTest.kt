package app.funput.funput.ui.kit.gestures

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TalkBack reaches every swipe action, since it cannot swipe the row. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class SwipeActionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `both swipe actions are offered to TalkBack in reading order`() {
        val swiped = mutableListOf<String>()
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SwipeActions(
                    modifier = Modifier.testTag("row"),
                    start = SwipeAction("Chặn", Color.Yellow) { swiped += "block" },
                    end = SwipeAction("Xoá", Color.Red) { swiped += "delete" },
                ) { BasicText("được") }
            }
        }

        val actions = compose.onNodeWithTag("row").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Chặn", "Xoá"), actions.map { it.label })
        actions.forEach { it.action() }
        assertEquals(listOf("block", "delete"), swiped)
    }

    @Test
    fun `a disabled row offers nothing`() {
        compose.setContent {
            FunputUiTheme(isDark = false) {
                SwipeToDelete("Xoá", onDelete = {}, modifier = Modifier.testTag("row"), enabled = false) {
                    BasicText("được")
                }
            }
        }

        val config = compose.onNodeWithTag("row").fetchSemanticsNode().config
        assertTrue(SemanticsActions.CustomActions !in config)
    }
}
