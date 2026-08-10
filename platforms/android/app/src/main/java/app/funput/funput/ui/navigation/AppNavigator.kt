package app.funput.funput.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * A back stack per tab, plus which tab is showing.
 *
 * One stack for the whole app cannot say what a tabbed app needs to say: switching tab is a move
 * sideways that must not be pushed anywhere, while a tab left half way into something has to still
 * be there on return.
 */
@Stable
internal class AppNavigator internal constructor(
    initialTab: TopLevelDestination = TopLevelDestination.Start,
    initialStacks: Map<TopLevelDestination, List<AppDestination>> = emptyMap(),
) {
    private val stacks = mutableStateMapOf<TopLevelDestination, List<AppDestination>>().apply {
        TopLevelDestination.entries.forEach { tab ->
            val restored = initialStacks[tab]?.takeIf { stack -> stack.isNotEmpty() }
            put(tab, restored ?: listOf(AppDestination.rootOf(tab)))
        }
    }

    var currentTab by mutableStateOf(initialTab)
        private set

    val currentDestination: AppDestination
        get() = stackOf(currentTab).last()

    /** Back only leaves the current tab once that tab is at its own root. */
    val canNavigateBack: Boolean
        get() = stackOf(currentTab).size > 1 || currentTab != TopLevelDestination.Start

    /**
     * Where back would land. A predictive back gesture draws that screen while the finger is still
     * down, which means knowing it before anything is popped.
     */
    val previousDestination: AppDestination?
        get() {
            val stack = stackOf(currentTab)
            if (stack.size > 1) return stack[stack.lastIndex - 1]
            if (currentTab == TopLevelDestination.Start) return null
            return stackOf(TopLevelDestination.Start).last()
        }

    internal fun stackOf(tab: TopLevelDestination): List<AppDestination> = stacks.getValue(tab)

    /** Switching tab keeps whatever that tab was showing. It is a move sideways, not a push. */
    fun selectTab(tab: TopLevelDestination) {
        currentTab = tab
    }

    fun navigate(destination: AppDestination) {
        currentTab = destination.tab
        val stack = stackOf(destination.tab)
        if (stack.last() != destination) stacks[destination.tab] = stack + destination
    }

    fun navigateBack(): Boolean {
        val stack = stackOf(currentTab)
        if (stack.size > 1) {
            stacks[currentTab] = stack.dropLast(1)
            return true
        }
        if (currentTab != TopLevelDestination.Start) {
            currentTab = TopLevelDestination.Start
            return true
        }
        return false
    }
}

@Composable
internal fun rememberAppNavigator(): AppNavigator =
    rememberSaveable(saver = AppNavigatorSaver) { AppNavigator() }
