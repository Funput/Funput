package app.funput.funput.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/** Small restorable back stack for the app's current pair of destinations. */
@Stable
internal class AppNavigator private constructor(
    initialBackStack: List<AppDestination>,
) {
    private val backStack = mutableStateListOf<AppDestination>().apply {
        addAll(initialBackStack.ifEmpty { listOf(AppDestination.SETTINGS) })
    }

    val currentDestination: AppDestination
        get() = backStack.last()

    val canNavigateBack: Boolean
        get() = backStack.size > 1

    constructor() : this(listOf(AppDestination.SETTINGS))

    fun navigate(destination: AppDestination) {
        if (destination != currentDestination) backStack += destination
    }

    fun navigateBack(): Boolean {
        if (!canNavigateBack) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    companion object {
        val Saver: Saver<AppNavigator, ArrayList<String>> = Saver(
            save = { navigator ->
                ArrayList(navigator.backStack.map(AppDestination::name))
            },
            restore = { savedBackStack ->
                AppNavigator(savedBackStack.map(AppDestination::valueOf))
            },
        )
    }
}

@Composable
internal fun rememberAppNavigator(): AppNavigator =
    rememberSaveable(saver = AppNavigator.Saver) { AppNavigator() }
