package app.funput.funput.ui.navigation

import androidx.compose.runtime.saveable.Saver

/**
 * Saves the tab and every tab's stack as flat strings, the only shapes a Bundle takes without
 * ceremony. Entry zero is the current tab; the rest are `TAB:DESTINATION` in stack order.
 *
 * Anything that no longer parses — a destination renamed between app versions, a stack saved
 * before a tab existed — is dropped, and that tab falls back to its root. A restored back stack is
 * not worth a crash on launch.
 */
internal val AppNavigatorSaver: Saver<AppNavigator, ArrayList<String>> = Saver(
    save = { navigator ->
        ArrayList<String>().apply {
            add(navigator.currentTab.name)
            TopLevelDestination.entries.forEach { tab ->
                navigator.stackOf(tab).forEach { destination -> add("${tab.name}:${destination.name}") }
            }
        }
    },
    restore = { saved ->
        val tab = saved.firstOrNull()?.let(::topLevelOrNull) ?: TopLevelDestination.Start
        val stacks = saved.drop(1)
            .mapNotNull(::destinationOrNull)
            .groupBy { destination -> destination.tab }
        AppNavigator(initialTab = tab, initialStacks = stacks)
    },
)

private fun topLevelOrNull(name: String): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { entry -> entry.name == name }

private fun destinationOrNull(entry: String): AppDestination? {
    val (tabName, destinationName) = entry.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
    val destination = AppDestination.entries.firstOrNull { it.name == destinationName } ?: return null
    return destination.takeIf { it.tab.name == tabName }
}
