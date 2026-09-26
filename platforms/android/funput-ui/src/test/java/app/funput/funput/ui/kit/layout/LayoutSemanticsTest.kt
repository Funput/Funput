package app.funput.funput.ui.kit.layout

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.funput.funput.ui.kit.R
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What TalkBack and switch access get from the screen frame. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK])
class LayoutSemanticsTest {
    @get:Rule
    val compose = createComposeRule()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    @Test
    fun `tabs are tabs, the first one selected, and a tap reports its index`() {
        var selected = -1
        compose.setContent { SampleScreen(isDark = false, onSelectTab = { selected = it }) }

        // "Cài đặt" is also the screen title, so match the tab by role as well as text.
        compose.onNode(hasText("Cài đặt") and isTab).assertIsSelected()
        compose.onNode(hasText("Giao diện") and isTab).assertIsNotSelected().performClick()

        assertEquals(1, selected)
    }

    @Test
    fun `the large title and every group header are headings`() {
        compose.setContent { SampleScreen(isDark = false) }

        val headings = compose.onAllNodes(isHeading()).fetchSemanticsNodes()
        assertTrue(headings.size >= 2)
        compose.onAllNodes(isHeading() and hasText("GÕ TIẾNG VIỆT")).fetchSemanticsNodes().let {
            assertEquals(1, it.size)
        }
    }

    @Test
    fun `the back button is labelled and calls back`() {
        var backs = 0
        compose.setContent { SampleScreen(isDark = false, onBack = { backs++ }) }

        val label = ApplicationProvider.getApplicationContext<Context>().getString(R.string.funput_ui_back)
        compose.onNodeWithContentDescription(label).performClick()

        assertEquals(1, backs)
    }
}
