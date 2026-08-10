package app.funput.funput.ui.settings.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Where a row sits in its [SettingsGroup], which decides how its corners are rounded. */
internal enum class RowPosition { FIRST, MIDDLE, LAST, ONLY }

/**
 * Material 3's grouped-list treatment: rows are separate shapes stacked with a hairline gap, the
 * ends of the group carry the large radius, and pressing a row swells its corners.
 *
 * The swell is what a plain ripple cannot express — the row reacts as an object rather than as a
 * lit rectangle. Material's own expressive components animate this from the theme's motion scheme,
 * but that scheme is internal to material3 1.4.0, so the spring is spelled out here instead.
 */
@Composable
internal fun rememberRowShape(position: RowPosition, interactionSource: InteractionSource): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val outer = animateCornerAsState(if (pressed) PressedOuterRadius else OuterRadius, pressed)
    val inner = animateCornerAsState(if (pressed) PressedInnerRadius else InnerRadius, pressed)
    val (top, bottom) = when (position) {
        RowPosition.FIRST -> outer.value to inner.value
        RowPosition.MIDDLE -> inner.value to inner.value
        RowPosition.LAST -> inner.value to outer.value
        RowPosition.ONLY -> outer.value to outer.value
    }
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * Springy on the way in, settled on the way out. A bouncy release reads as the row rejecting the
 * touch, so only the press itself overshoots.
 */
@Composable
private fun animateCornerAsState(target: Dp, pressed: Boolean): State<Dp> = animateDpAsState(
    targetValue = target,
    animationSpec = spring(
        dampingRatio = if (pressed) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    ),
    label = "settings-row-corner",
)

/** Positions for a group of [count] rows, so call sites do not index-juggle. */
internal fun rowPositions(count: Int): List<RowPosition> = when (count) {
    0 -> emptyList()
    1 -> listOf(RowPosition.ONLY)
    else -> List(count) { index ->
        when (index) {
            0 -> RowPosition.FIRST
            count - 1 -> RowPosition.LAST
            else -> RowPosition.MIDDLE
        }
    }
}

private val OuterRadius = 20.dp
private val InnerRadius = 4.dp
private val PressedOuterRadius = 28.dp
private val PressedInnerRadius = 12.dp
