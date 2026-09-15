package app.funput.funput.ui.about.licenses

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import app.funput.funput.ui.about.AboutRoute
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.rememberAppNavigator
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Rule
import org.junit.Test

class LicensesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun opensFullOfflineNoticeAndReturnsToAbout() {
        compose.setContent {
            val navigator = rememberAppNavigator()
            FunputTheme {
                if (navigator.currentDestination == AppDestination.THIRD_PARTY_LICENSES) {
                    LicensesRoute { navigator.navigateBack() }
                } else {
                    AboutRoute { navigator.navigate(AppDestination.THIRD_PARTY_LICENSES) }
                }
            }
        }
        scrollToLicenses()
        compose.onNodeWithText("Giấy phép và ghi công cho từ điển đi kèm").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText("SCOWL", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        for (source in listOf("SCOWL", "Google", "LDNOOBW", "Funput")) {
            compose.onNodeWithText(source, substring = true).assertExists()
        }
        compose.onNodeWithText("Quay lại").performClick()
        scrollToLicenses()
        compose.onNodeWithText("Giấy phép và ghi công cho từ điển đi kèm").assertIsDisplayed()
    }
    private fun scrollToLicenses() {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Giấy phép và ghi công cho từ điển đi kèm"))
    }
}
