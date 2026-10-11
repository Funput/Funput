package app.funput.funput.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What a destination remembers lives exactly as long as the destination is on a back stack.
 *
 * The theme studio used to come back with the draft of its previous visit, because the state of a
 * popped destination was kept and rememberSaveable restores it without checking its inputs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class AppNavDisplayStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val navigator = AppNavigator()

    @Test
    fun `a popped destination starts fresh on its next visit`() {
        showCounters()
        compose.runOnIdle { navigator.navigate(AppDestination.CREATE_CUSTOM_THEME) }
        tapCounter(AppDestination.CREATE_CUSTOM_THEME)
        counter(AppDestination.CREATE_CUSTOM_THEME).assertTextEquals("1")

        compose.runOnIdle { navigator.navigateBack() }
        compose.waitForIdle()
        compose.runOnIdle { navigator.navigate(AppDestination.CREATE_CUSTOM_THEME) }

        counter(AppDestination.CREATE_CUSTOM_THEME).assertTextEquals("0")
    }

    @Test
    fun `a destination still on a back stack keeps its state`() {
        showCounters()
        tapCounter(AppDestination.SETTINGS)

        compose.runOnIdle { navigator.navigate(AppDestination.SPEECH) }
        compose.waitForIdle()
        compose.runOnIdle { navigator.navigateBack() }

        counter(AppDestination.SETTINGS).assertTextEquals("1")
    }

    private fun showCounters() {
        compose.setContent {
            AppNavDisplay(navigator) { destination ->
                var taps by rememberSaveable { mutableIntStateOf(0) }
                BasicText(
                    text = taps.toString(),
                    modifier = Modifier.testTag(destination.name).clickable { taps += 1 },
                )
            }
        }
    }

    private fun tapCounter(destination: AppDestination) = counter(destination).performClick()

    private fun counter(destination: AppDestination) = compose.onNodeWithTag(destination.name)
}
