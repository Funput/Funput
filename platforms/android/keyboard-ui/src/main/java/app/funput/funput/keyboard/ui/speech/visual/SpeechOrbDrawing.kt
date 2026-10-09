package app.funput.funput.keyboard.ui.speech.visual

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import kotlin.math.PI
import kotlin.math.sin

internal fun DrawScope.drawSpeechOrb(style: SpeechPanelStyle, stage: SpeechPanelStage, phase: Float) {
    val flow = sin(phase * 2f * PI).toFloat()
    val radius = size.minDimension * (.36f + flow * .008f)
    val tint = Color(style.orbTint)
    val deep = lerp(tint, Color.Black, .48f)
    val light = lerp(tint, Color.White, .65f)
    val circle = Path().apply {
        addOval(Rect(center - Offset(radius, radius), center + Offset(radius, radius)))
    }
    drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = .17f), Color.Transparent),
        center, radius * 1.4f), radius * 1.4f)
    clipPath(circle) {
        drawCircle(Brush.linearGradient(listOf(light, tint, deep),
            center - Offset(radius, radius), center + Offset(radius, radius)), radius)
        if (stage != SpeechPanelStage.ERROR) {
            drawRibbon(center, radius, flow, light.copy(alpha = .65f), deep.copy(alpha = .12f))
            drawRibbon(center + Offset(0f, radius * .38f), radius, -flow,
                light.copy(alpha = .92f), tint.copy(alpha = .05f))
        }
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .6f), Color.Transparent),
            center - Offset(radius * .45f, radius * .52f), radius * 1.25f), radius)
    }
    drawCircle(light.copy(alpha = .5f), radius, style = Stroke(size.minDimension * .006f))
    if (stage == SpeechPanelStage.ERROR) {
        val ink = if (tint.luminance() > .45f) deep else light
        drawLine(ink, center - Offset(0f, radius * .38f), center + Offset(0f, radius * .1f),
            radius * .08f, StrokeCap.Round)
        drawCircle(ink, radius * .05f, center + Offset(0f, radius * .35f))
    }
}

private fun DrawScope.drawRibbon(origin: Offset, radius: Float, flow: Float, light: Color, dark: Color) {
    val edge = radius * 1.2f
    val path = Path().apply {
        moveTo(origin.x - edge, origin.y + radius * .22f)
        cubicTo(origin.x - radius * .35f, origin.y - radius * (.85f + flow * .16f),
            origin.x + radius * .3f, origin.y + radius * (.8f - flow * .16f),
            origin.x + edge, origin.y - radius * .24f)
        lineTo(origin.x + edge, origin.y + edge)
        lineTo(origin.x - edge, origin.y + edge)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(light, dark), origin.y - radius * .6f, origin.y + radius))
}
