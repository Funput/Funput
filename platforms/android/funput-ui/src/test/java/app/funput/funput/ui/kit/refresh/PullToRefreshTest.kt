package app.funput.funput.ui.kit.refresh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pulling a list down from its top asks for a refresh once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class PullToRefreshTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `pulling down from the top refreshes`() {
        var refreshes = 0
        compose.setContent {
            FunputUiTheme(isDark = false) {
                FunputPullToRefresh(refreshing = false, onRefresh = { refreshes += 1 }) {
                    LazyColumn(Modifier.fillMaxSize()) { items(30) { BasicText("Dòng $it") } }
                }
            }
        }

        compose.onNodeWithTag(FunputPullToRefreshTag).performTouchInput { swipeDown(startY = top + 10f, endY = bottom) }

        compose.runOnIdle { assertEquals(1, refreshes) }
    }
}
