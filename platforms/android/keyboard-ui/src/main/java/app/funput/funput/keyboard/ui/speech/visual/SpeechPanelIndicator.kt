package app.funput.funput.keyboard.ui.speech.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage

/** A static state illustration, never an invented audio level or a recording side effect. */
@Composable
internal fun SpeechPanelIndicator(stage: SpeechPanelStage, style: SpeechPanelStyle) {
    Canvas(Modifier.size(64.dp)) {
        val unit = size.minDimension / 64f
        val color = Color(style.indicator)
        drawCircle(color.copy(alpha = .09f))
        drawCircle(color.copy(alpha = .18f), radius = 29 * unit, style = Stroke(unit))
        val stroke = Stroke(2.4f * unit, cap = StrokeCap.Round)
        if (stage == SpeechPanelStage.ERROR) {
            drawLine(color, Offset(32 * unit, 20 * unit), Offset(32 * unit, 34 * unit), stroke.width, StrokeCap.Round)
            drawCircle(color, 1.5f * unit, Offset(32 * unit, 41 * unit))
        } else if (stage == SpeechPanelStage.LISTENING) {
            drawRoundRect(color, Offset(27 * unit, 17 * unit), Size(10 * unit, 21 * unit),
                CornerRadius(5 * unit), style = stroke)
            drawArc(color, 0f, 180f, false, Offset(22 * unit, 25 * unit), Size(20 * unit, 20 * unit), style = stroke)
            drawLine(color, Offset(32 * unit, 45 * unit), Offset(32 * unit, 49 * unit), stroke.width, StrokeCap.Round)
            drawLine(color, Offset(27 * unit, 49 * unit), Offset(37 * unit, 49 * unit), stroke.width, StrokeCap.Round)
        } else {
            drawArc(color, -90f, 270f, false, Offset(21 * unit, 21 * unit), Size(22 * unit, 22 * unit), style = stroke)
            drawCircle(color, 2 * unit, Offset(43 * unit, 32 * unit))
        }
    }
}
