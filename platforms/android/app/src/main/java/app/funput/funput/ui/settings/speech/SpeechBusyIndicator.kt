package app.funput.funput.ui.settings.speech

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/** A small turning arc for work the system is doing: checking or downloading a language. */
@Composable
internal fun SpeechBusyIndicator(modifier: Modifier = Modifier) {
    val color = FunputUi.colors.secondaryLabel
    val turn by rememberInfiniteTransition(label = "speech-busy").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "speech-busy-turn",
    )
    Canvas(modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        drawArc(
            color = color,
            startAngle = turn,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
        )
    }
}
