package app.funput.funput.ui.kit.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A plain content card: the card colour, continuous 22dp corners and a hairline.
 *
 * No padding by default, because rows inside a card run edge to edge (their ripple and dividers
 * need the full width); pass [contentPadding] for free-form content.
 */
@Composable
fun FunputCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FunputUi.colors
    val shape = FunputUi.shapes.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBackground)
            .border(FunputUi.spacing.cardStrokeWidth, colors.cardStroke, shape)
            .padding(contentPadding),
        content = content,
    )
}
