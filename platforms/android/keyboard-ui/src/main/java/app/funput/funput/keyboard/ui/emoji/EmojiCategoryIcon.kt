package app.funput.funput.keyboard.ui.emoji

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

@Composable
internal fun EmojiCategoryIcon(category: EmojiCategory, tint: Color) {
    val path = remember(category) { EmojiCategoryPaths.create(category) }
    Canvas(Modifier.size(22.dp)) {
        scale(size.minDimension / Viewport, pivot = Offset.Zero) {
            drawPath(
                path,
                tint,
                style = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private object EmojiCategoryPaths {
    fun create(category: EmojiCategory) = Path().apply {
        when (category) {
            EmojiCategory.RECENT -> clock()
            EmojiCategory.SMILEYS_PEOPLE -> smile()
            EmojiCategory.ANIMALS_NATURE -> leaf()
            EmojiCategory.FOOD_DRINK -> food()
            EmojiCategory.ACTIVITIES -> ball()
            EmojiCategory.TRAVEL_PLACES -> location()
            EmojiCategory.OBJECTS -> bulb()
            EmojiCategory.SYMBOLS -> heart()
            EmojiCategory.FLAGS -> flag()
        }
    }

    private fun Path.clock() {
        addOval(Rect(3.5f, 3.5f, 20.5f, 20.5f))
        moveTo(12f, 7f); lineTo(12f, 12f); lineTo(16f, 12f)
    }

    private fun Path.smile() {
        addOval(Rect(3.5f, 3.5f, 20.5f, 20.5f))
        addOval(Rect(7.4f, 8f, 8.6f, 9.2f)); addOval(Rect(15.4f, 8f, 16.6f, 9.2f))
        moveTo(7.5f, 13f); cubicTo(9f, 17f, 15f, 17f, 16.5f, 13f)
    }

    private fun Path.leaf() {
        moveTo(5f, 18.5f); cubicTo(5f, 10f, 10f, 5f, 19f, 4.5f)
        cubicTo(18.5f, 13.5f, 13.5f, 18.5f, 5f, 18.5f)
        moveTo(6f, 18f); lineTo(15.5f, 8.5f)
    }

    private fun Path.food() {
        moveTo(5f, 4f); lineTo(5f, 10f); cubicTo(5f, 12f, 9f, 12f, 9f, 10f)
        lineTo(9f, 4f); moveTo(7f, 4f); lineTo(7f, 20f)
        moveTo(15f, 20f); lineTo(15f, 4f); cubicTo(20f, 7f, 19f, 13f, 15f, 13f)
    }

    private fun Path.ball() {
        addOval(Rect(3.5f, 3.5f, 20.5f, 20.5f))
        moveTo(12f, 3.5f); lineTo(12f, 20.5f)
        moveTo(4.5f, 8f); cubicTo(8f, 10f, 16f, 10f, 19.5f, 8f)
        moveTo(4.5f, 16f); cubicTo(8f, 14f, 16f, 14f, 19.5f, 16f)
    }

    private fun Path.location() {
        moveTo(12f, 21f); cubicTo(9f, 17f, 5.5f, 13f, 5.5f, 9.5f)
        cubicTo(5.5f, 1.5f, 18.5f, 1.5f, 18.5f, 9.5f)
        cubicTo(18.5f, 13f, 15f, 17f, 12f, 21f)
        addOval(Rect(9.5f, 7f, 14.5f, 12f))
    }

    private fun Path.bulb() {
        moveTo(9f, 17f); lineTo(9f, 15f); cubicTo(3f, 9f, 7f, 3f, 12f, 3f)
        cubicTo(17f, 3f, 21f, 9f, 15f, 15f); lineTo(15f, 17f)
        moveTo(9f, 19f); lineTo(15f, 19f); moveTo(10f, 21f); lineTo(14f, 21f)
    }

    private fun Path.heart() {
        moveTo(12f, 20f); cubicTo(9f, 17f, 4f, 13.5f, 4f, 8.5f)
        cubicTo(4f, 3f, 10f, 3f, 12f, 7f); cubicTo(14f, 3f, 20f, 3f, 20f, 8.5f)
        cubicTo(20f, 13.5f, 15f, 17f, 12f, 20f)
    }

    private fun Path.flag() {
        moveTo(6f, 21f); lineTo(6f, 3f); moveTo(6f, 4f)
        cubicTo(10f, 2f, 14f, 6f, 19f, 4f); lineTo(19f, 13f)
        cubicTo(14f, 15f, 10f, 11f, 6f, 13f)
    }
}

private const val Viewport = 24f
