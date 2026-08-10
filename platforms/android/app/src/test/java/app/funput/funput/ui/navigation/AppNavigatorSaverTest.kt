package app.funput.funput.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The saver runs on process death, when nobody is watching and a throw is a crash on launch. Its
 * job is to restore what it can and shrug off the rest.
 */
class AppNavigatorSaverTest {

    @Test
    fun `restores the tab and every stack`() {
        val navigator = AppNavigator()
        navigator.navigate(AppDestination.CREATE_CUSTOM_THEME)

        val restored = roundTrip(navigator)

        assertEquals(TopLevelDestination.APPEARANCE, restored.currentTab)
        assertEquals(AppDestination.CREATE_CUSTOM_THEME, restored.currentDestination)
        assertEquals(
            listOf(AppDestination.THEME_GALLERY, AppDestination.CREATE_CUSTOM_THEME),
            restored.stackOf(TopLevelDestination.APPEARANCE),
        )
    }

    @Test
    fun `a tab left alone comes back at its root`() {
        val restored = roundTrip(AppNavigator())

        assertEquals(
            listOf(AppDestination.THEME_GALLERY),
            restored.stackOf(TopLevelDestination.APPEARANCE),
        )
    }

    @Test
    fun `entries that no longer parse are dropped, not thrown on`() {
        // What an app update looks like to the saver: a renamed destination, a tab that is gone,
        // and a line in the wrong shape entirely.
        val saved = arrayListOf(
            "SETTINGS",
            "SETTINGS:SETTINGS",
            "APPEARANCE:THEME_GALLERY",
            "APPEARANCE:SOME_SCREEN_THAT_WAS_RENAMED",
            "A_TAB_THAT_NO_LONGER_EXISTS:SETTINGS",
            "nonsense",
        )

        val restored = requireNotNull(AppNavigatorSaver.restore(saved))

        assertEquals(TopLevelDestination.SETTINGS, restored.currentTab)
        assertEquals(listOf(AppDestination.SETTINGS), restored.stackOf(TopLevelDestination.SETTINGS))
        assertEquals(
            listOf(AppDestination.THEME_GALLERY),
            restored.stackOf(TopLevelDestination.APPEARANCE),
        )
    }

    @Test
    fun `an unknown saved tab falls back to the start tab`() {
        val restored = requireNotNull(AppNavigatorSaver.restore(arrayListOf("NOT_A_TAB")))

        assertEquals(TopLevelDestination.Start, restored.currentTab)
    }

    private fun roundTrip(navigator: AppNavigator): AppNavigator {
        val scope = object : androidx.compose.runtime.saveable.SaverScope {
            override fun canBeSaved(value: Any) = true
        }
        val saved = requireNotNull(with(AppNavigatorSaver) { scope.save(navigator) })
        return requireNotNull(AppNavigatorSaver.restore(saved))
    }
}
