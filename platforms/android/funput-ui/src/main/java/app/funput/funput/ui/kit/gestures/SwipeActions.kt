package app.funput.funput.ui.kit.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import app.funput.funput.ui.kit.theme.FunputUi

/** What swiping a row one way does: [label] on [color], then [onSwipe]. */
@Immutable
data class SwipeAction(val label: String, val color: Color, val onSwipe: () -> Unit)

/**
 * Swipe a row toward the end for [start] or toward the start for [end], revealing the action's
 * label on its colour.
 *
 * A swipe only asks: the action runs and the row springs back, so the caller confirms a
 * destructive one (a dialog) before anything is removed. Material's swipe-to-dismiss is borrowed
 * for the gesture and thresholds; TalkBack users get each action as a custom action, since they
 * cannot swipe the row.
 */
@Composable
fun SwipeActions(
    modifier: Modifier = Modifier,
    start: SwipeAction? = null,
    end: SwipeAction? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val current = rememberUpdatedState(start to end)
    LaunchedEffect(state.currentValue) {
        val (leading, trailing) = current.value
        val action = when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> leading
            SwipeToDismissBoxValue.EndToStart -> trailing
            SwipeToDismissBoxValue.Settled -> return@LaunchedEffect
        }
        action?.onSwipe?.invoke()
        state.snapTo(SwipeToDismissBoxValue.Settled)
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enabled && start != null,
        enableDismissFromEndToStart = enabled && end != null,
        backgroundContent = {
            // Drawn only mid-swipe: at rest, the card's anti-aliased corners would show a coloured rim.
            when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> start?.let { ActionBackground(it, Alignment.CenterStart) }
                SwipeToDismissBoxValue.EndToStart -> end?.let { ActionBackground(it, Alignment.CenterEnd) }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        },
        modifier = modifier.semantics {
            if (enabled) {
                customActions = listOfNotNull(start, end).map { CustomAccessibilityAction(it.label) { it.onSwipe(); true } }
            }
        },
    ) {
        Box(Modifier.background(FunputUi.colors.cardBackground)) { content() }
    }
}

@Composable
private fun ActionBackground(action: SwipeAction, alignment: Alignment) {
    Box(
        contentAlignment = alignment,
        modifier = Modifier.fillMaxSize().background(action.color)
            .padding(horizontal = FunputUi.spacing.cardPadding)
            .clearAndSetSemantics {},
    ) {
        val style = FunputUi.typography.label.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
        BasicText(action.label, style = style)
    }
}
