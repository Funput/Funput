package app.funput.funput.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Remembers which rows have already arrived, so a page animates itself in once and not again.
 *
 * Without this the animation belongs to the item rather than to the page: scrolling a row off and
 * back on would play it again, which reads as the list being rebuilt under the finger.
 */
@Composable
internal fun rememberEntryTracker(): EntryTracker = rememberSaveable(saver = EntryTracker.Saver) {
    EntryTracker()
}

internal class EntryTracker(seen: Set<Int> = emptySet()) {
    private val arrived = seen.toMutableSet()

    fun hasArrived(index: Int): Boolean = index in arrived

    fun markArrived(index: Int) {
        arrived += index
    }

    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<EntryTracker, ArrayList<Int>>(
            save = { tracker -> ArrayList(tracker.arrived) },
            restore = { seen -> EntryTracker(seen.toSet()) },
        )
    }
}

/**
 * Rises and fades a row into place, a beat after the one above it.
 *
 * Rows used to be simply present the instant a page opened, which is the difference between a
 * screen that arrives and a screen that was always there.
 */
@Composable
internal fun Modifier.staggeredEntry(index: Int, tracker: EntryTracker): Modifier {
    if (tracker.hasArrived(index)) return this
    var entered by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = EnterMillis),
        finishedListener = { tracker.markArrived(index) },
        label = "entry-$index",
    )
    val rise = with(LocalDensity.current) { RiseFrom.toPx() }
    LaunchedEffectOnce(index) { entered = true }
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * rise
    }
}

@Composable
private fun LaunchedEffectOnce(index: Int, onReady: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Only the first screenful staggers; past that the delay would outlast the scroll.
        delay(StepMillis * index.coerceAtMost(MaxStaggered))
        onReady()
    }
}

private const val EnterMillis = 260
private const val StepMillis = 45L
private const val MaxStaggered = 6
private val RiseFrom = 12.dp
