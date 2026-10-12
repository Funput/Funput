package app.funput.funput.ui.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import kotlin.coroutines.cancellation.CancellationException

/**
 * Draws the current destination and animates the moves between them.
 *
 * Back is seekable rather than played back after the fact: [PredictiveBackHandler] feeds the
 * gesture's progress into a [SeekableTransitionState], so the previous screen is really on screen
 * under the finger and follows it, forward and backward, until the gesture commits or is dropped.
 * Android animates the app window this way by default; without this the inside of the app stayed
 * frozen until the moment back landed.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun AppNavDisplay(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    content: @Composable (AppDestination) -> Unit,
) {
    val current = navigator.currentDestination
    val transitionState = remember { SeekableTransitionState(current) }
    val transition = rememberTransition(transitionState, label = "app-destination")

    // Navigation that did not come from the gesture — a tap, or the gesture committing — arrives
    // here as a plain target change and animates at its own pace.
    LaunchedEffect(current) {
        if (transitionState.currentState != current) transitionState.animateTo(current)
    }

    PredictiveBackHandler(enabled = navigator.canNavigateBack) { progress ->
        val previous = navigator.previousDestination ?: return@PredictiveBackHandler
        try {
            progress.collect { event ->
                transitionState.seekTo(fraction = event.progress, targetState = previous)
            }
            navigator.navigateBack()
        } catch (cancelled: CancellationException) {
            // The user let go before committing, so walk the transition back to where it started.
            transitionState.snapTo(navigator.currentDestination)
            throw cancelled
        }
    }

    // A destination leaves composition when it is animated away, taking its scroll position and
    // every other remembered bit with it. Holding that state per destination is what makes back
    // land where the user left off instead of at the top of the page.
    val stateHolder = rememberSaveableStateHolder()
    // A popped destination must not keep that state, though. The next visit is a new one, and
    // rememberSaveable restores by position without checking its inputs, so a leftover draft from
    // the last visit would be handed to whatever the screen opens next. Removing it while the
    // screen is still animating out is safe: the holder then simply skips saving it on dispose.
    val liveDestinations = navigator.liveDestinations
    LaunchedEffect(liveDestinations) {
        AppDestination.entries.filterNot(liveDestinations::contains).forEach(stateHolder::removeState)
    }
    SharedTransitionLayout(modifier = modifier) {
        transition.AnimatedContent(
            transitionSpec = { destinationTransition() },
        ) { destination ->
            val scopes = SharedTransitionScopes(this@SharedTransitionLayout, this)
            CompositionLocalProvider(LocalSharedTransitionScopes provides scopes) {
                stateHolder.SaveableStateProvider(destination) { content(destination) }
            }
        }
    }
}
