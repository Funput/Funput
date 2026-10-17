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
            "KEYBOARD",
            "KEYBOARD:KEYBOARD",
            "APPEARANCE:THEME_GALLERY",
            "APPEARANCE:SOME_SCREEN_THAT_WAS_RENAMED",
            "A_TAB_THAT_NO_LONGER_EXISTS:KEYBOARD",
            "nonsense",
        )

        val restored = requireNotNull(AppNavigatorSaver.restore(saved))

        assertEquals(TopLevelDestination.KEYBOARD, restored.currentTab)
        assertEquals(listOf(AppDestination.KEYBOARD), restored.stackOf(TopLevelDestination.KEYBOARD))
        assertEquals(
            listOf(AppDestination.THEME_GALLERY),
            restored.stackOf(TopLevelDestination.APPEARANCE),
        )
    }

    @Test
    fun `stacks saved before the keyboard hub and the me tab come back at the tab roots`() {
        // Saved by the version with one long settings screen and an about tab.
        val saved = arrayListOf(
            "ABOUT",
            "SETTINGS:SETTINGS",
            "SETTINGS:SHORTCUTS",
            "SETTINGS:SPEECH",
            "ABOUT:ABOUT",
            "ABOUT:THIRD_PARTY_LICENSES",
        )

        val restored = requireNotNull(AppNavigatorSaver.restore(saved))

        assertEquals(TopLevelDestination.Start, restored.currentTab)
        TopLevelDestination.entries.forEach { tab ->
            assertEquals(listOf(AppDestination.rootOf(tab)), restored.stackOf(tab))
        }
    }

    @Test
    fun `a stack that lost its root gets it back in front`() {
        val saved = arrayListOf("ME", "ME:THIRD_PARTY_LICENSES")

        val restored = requireNotNull(AppNavigatorSaver.restore(saved))

        assertEquals(
            listOf(AppDestination.ME, AppDestination.THIRD_PARTY_LICENSES),
            restored.stackOf(TopLevelDestination.ME),
        )
        assertEquals(AppDestination.ME, restored.previousDestination)
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
