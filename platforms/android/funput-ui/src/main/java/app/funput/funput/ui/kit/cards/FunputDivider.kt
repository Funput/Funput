package app.funput.funput.ui.kit.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * The hairline between rows of a card, indented by [startInset] so it lines up with the row text
 * rather than the icon, as grouped lists do.
 */
@Composable
fun FunputDivider(modifier: Modifier = Modifier, startInset: Dp = FunputUi.spacing.cardPadding) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(0.5.dp)
            .background(FunputUi.colors.separator),
    )
}
