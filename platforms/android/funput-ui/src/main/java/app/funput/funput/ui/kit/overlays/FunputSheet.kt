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
import app.funput.funput.ui.kit.theme.ProvideElevatedColors

/**
 * A bottom sheet for a focused task, typically a picker: a title, then content. The sheet and its
 * cards use the raised surface colours, so in dark mode the sheet stands off the scrim and the
 * `FunputSection`s inside it still stand off the sheet.
 *
 * Material's sheet, restyled. Drag to dismiss, scrim tap, predictive back and window insets are
 * handled there, and that is behaviour worth borrowing rather than rebuilding.
 */
@Composable
fun FunputSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideElevatedColors { SheetContent(onDismiss, title, content) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetContent(onDismiss: () -> Unit, title: String?, content: @Composable ColumnScope.() -> Unit) {
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
