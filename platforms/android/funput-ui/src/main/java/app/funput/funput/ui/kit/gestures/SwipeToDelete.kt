package app.funput.funput.ui.kit.gestures

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Swipe a row to the start to ask for its deletion, revealing [label] on the destructive colour.
 * [onDelete] only asks; see [SwipeActions], which this is the one-action case of.
 */
@Composable
fun SwipeToDelete(
    label: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    SwipeActions(
        modifier = modifier,
        end = SwipeAction(label, FunputUi.colors.destructive, onDelete),
        enabled = enabled,
        content = content,
    )
}
