package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange
import app.funput.funput.ui.theme.BrandPink
import app.funput.funput.ui.theme.BrandPurple
import app.funput.funput.ui.theme.FunputShapes

/** State of a single onboarding step in the keyboard setup journey. */
internal enum class StepState { DONE, ACTIVE, UPCOMING }

/** Shared brand gradient stops used across the setup card badges, border and button. */
internal val BrandSweep = listOf(BrandOrange, BrandPink, BrandPurple, BrandBlue)

internal val KeyboardSetupCardShape = FunputShapes.large

/**
 * The setup container.
 *
 * It used to stack three decorations — a raised surface, a brand wash over it, and a rainbow
 * hairline around that — plus a gradient button and gradient step badges. It was drowned out by
 * eight saturated icon tiles; once those became tonal it was the only thing left shouting.
 *
 * What it needs is to be the one thing on the page asking for attention, and `primaryContainer`
 * says that in the scheme's own voice — so it follows a wallpaper palette too, which a fixed
 * rainbow never could.
 */
@Composable
internal fun Modifier.heroCard(): Modifier = fillMaxWidth()
    .clip(KeyboardSetupCardShape)
    .background(MaterialTheme.colorScheme.primaryContainer)
    .padding(18.dp)

/** A flat single-color [Brush], for places that only accept a gradient. */
internal fun solidBrush(color: Color): Brush = Brush.linearGradient(listOf(color, color))
