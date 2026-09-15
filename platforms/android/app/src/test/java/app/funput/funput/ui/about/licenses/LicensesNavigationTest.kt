package app.funput.funput.ui.about.licenses

import androidx.compose.runtime.saveable.SaverScope
import app.funput.funput.ui.navigation.AppDestination
import app.funput.funput.ui.navigation.AppNavigator
import app.funput.funput.ui.navigation.AppNavigatorSaver
import app.funput.funput.ui.navigation.TopLevelDestination
import org.junit.Assert.*
import org.junit.Test

class LicensesNavigationTest {
    @Test fun backAndRestorationKeepTheAboutStack() {
        val navigator = AppNavigator()
        navigator.navigate(AppDestination.THIRD_PARTY_LICENSES)
        navigator.selectTab(TopLevelDestination.SETTINGS)
        navigator.selectTab(TopLevelDestination.ABOUT)
        val saved = with(AppNavigatorSaver) { SaverScope { true }.save(navigator) }
        val restored = requireNotNull(AppNavigatorSaver.restore(requireNotNull(saved)))
        assertEquals(AppDestination.THIRD_PARTY_LICENSES, restored.currentDestination)
        assertTrue(restored.navigateBack())
        assertEquals(AppDestination.ABOUT, restored.currentDestination)
    }
}
