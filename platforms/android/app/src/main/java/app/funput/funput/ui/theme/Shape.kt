package app.funput.funput.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Funput's shape scale. Rounder than the Material baseline by one step across the board, which is
 * what the settings screens were already doing by hand before every corner radius was a literal.
 *
 * Read it through `MaterialTheme.shapes` rather than re-declaring radii at call sites, so a change
 * here reaches Material components (sheets, dialogs, menus) and app surfaces alike.
 */
internal val FunputShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Fully rounded ends, for pills and step badges. Not part of [Shapes], which has no such slot. */
internal val PillShape = RoundedCornerShape(percent = 50)
