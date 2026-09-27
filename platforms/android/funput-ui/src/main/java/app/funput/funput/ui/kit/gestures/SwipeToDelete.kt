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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Swipe a row to the start to ask for its deletion, revealing [label] on the destructive colour.
 *
 * The swipe only asks: [onDelete] is called and the row springs back, so the caller confirms
 * (a dialog) before anything is removed, and cancelling leaves the row exactly where it was.
 * Material's swipe-to-dismiss is borrowed for the gesture and thresholds; TalkBack users get the
 * same request as a custom action named [label], since they cannot swipe the row.
 */
@Composable
fun SwipeToDelete(
    label: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            // Drawn only mid-swipe: at rest, the card's anti-aliased corners would show a red rim.
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) DeleteBackground(label)
        },
        modifier = modifier.semantics {
            if (enabled) customActions = listOf(CustomAccessibilityAction(label) { onDelete(); true })
        },
    ) {
        Box(Modifier.background(FunputUi.colors.cardBackground)) { content() }
    }
}

@Composable
private fun DeleteBackground(label: String) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier.fillMaxSize().background(FunputUi.colors.destructive)
            .padding(horizontal = FunputUi.spacing.cardPadding)
            .clearAndSetSemantics {},
    ) {
        val style = FunputUi.typography.label.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
        BasicText(label, style = style)
    }
}
