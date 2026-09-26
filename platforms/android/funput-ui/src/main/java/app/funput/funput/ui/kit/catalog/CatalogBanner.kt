package app.funput.funput.ui.kit.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/** A gradient card with a white label, standing in for a theme thumbnail in the catalog. */
@Composable
internal fun CatalogBanner(label: String, colors: List<Color>) {
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(FunputUi.shapes.card)
            .background(Brush.linearGradient(colors))
            .padding(FunputUi.spacing.cardPadding),
    ) {
        BasicText(label, style = FunputUi.typography.title.copy(color = Color.White))
    }
}
