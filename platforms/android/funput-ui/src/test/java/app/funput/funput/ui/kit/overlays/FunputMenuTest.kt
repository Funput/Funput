package app.funput.funput.ui.kit.overlays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class FunputMenuTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the current choice is selected and picking one closes the menu first`() {
        var expanded by mutableStateOf(true)
        val events = mutableListOf<String>()
        compose.setContent {
            FunputUiTheme(isDark = false) {
                FunputMenu(
                    expanded = expanded,
                    onDismiss = { events += "dismiss"; expanded = false },
                    items = listOf(
                        FunputMenuItem("Dùng nhiều nhất", { events += "most" }, selected = true),
                        FunputMenuItem("Mới dùng gần đây", { events += "recent" }),
                    ),
                )
            }
        }

        compose.onNodeWithText("Dùng nhiều nhất").assertIsSelected()
        compose.onNodeWithText("Mới dùng gần đây").assertIsNotSelected().performClick()
        compose.waitForIdle()

        assertEquals(listOf("dismiss", "recent"), events)
        compose.onNodeWithText("Mới dùng gần đây").assertDoesNotExist()
    }
}
