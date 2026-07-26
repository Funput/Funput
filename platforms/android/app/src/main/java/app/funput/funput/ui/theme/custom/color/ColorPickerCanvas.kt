package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

/** Saturation across, value down, over the given hue. */
@Composable
internal fun SaturationValueField(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .clip(RoundedCornerShape(12.dp))
            .positionInput { offset, size ->
                onChange(
                    (offset.x / size.width).coerceIn(0f, 1f),
                    1f - (offset.y / size.height).coerceIn(0f, 1f),
                )
            },
    ) {
        drawRect(
            Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))),
        )
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val radius = ThumbRadius.toPx()
        drawCircle(
            color = Color.White,
            radius = radius,
            // A white or fully dark color sits in a corner, where an unclamped thumb is clipped
            // away and the user cannot see what is selected.
            center = Offset(
                (saturation * size.width).coerceIn(radius, size.width - radius),
                ((1f - value) * size.height).coerceIn(radius, size.height - radius),
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = ThumbStroke.toPx()),
        )
    }
}

/** A horizontal bar the user scrubs, used for hue and for alpha. */
@Composable
internal fun GradientBar(
    colors: List<Color>,
    position: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .clip(RoundedCornerShape(8.dp))
            .positionInput { offset, size -> onChange((offset.x / size.width).coerceIn(0f, 1f)) },
    ) {
        drawRect(Brush.horizontalGradient(colors))
        val radius = ThumbRadius.toPx()
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(
                (position * size.width).coerceIn(radius, size.width - radius),
                size.height / 2f,
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = ThumbStroke.toPx()),
        )
    }
}

/** Reports tap and drag positions alike, so a scrub works whether or not the finger moves. */
private fun Modifier.positionInput(
    onPosition: (Offset, androidx.compose.ui.unit.IntSize) -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures { offset -> onPosition(offset, size) }
    }
    .pointerInput(Unit) {
        detectDragGestures { change, _ -> onPosition(change.position, size) }
    }

internal val HueColors: List<Color> = List(HueStops) { index ->
    Color.hsv(index * (360f / (HueStops - 1)), 1f, 1f)
}

private const val HueStops = 13
private val FieldHeight = 180.dp
private val BarHeight = 28.dp
private val ThumbRadius = 8.dp
private val ThumbStroke = 2.dp
