package app.funput.funput.ui.kit.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A bottom sheet for a focused task, typically a picker: a title, then content on the screen
 * background so `FunputSection`s inside it look like the rest of the app.
 *
 * Material's sheet, restyled. Drag to dismiss, scrim tap, predictive back and window insets are
 * handled there, and that is behaviour worth borrowing rather than rebuilding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunputSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FunputUi.colors
    val spacing = FunputUi.spacing
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.groupedBackground,
        contentColor = colors.label,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.large),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.pageMargin, end = spacing.pageMargin, bottom = spacing.section),
        ) {
            title?.let {
                BasicText(
                    text = it,
                    style = FunputUi.typography.title.copy(color = colors.label),
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}
