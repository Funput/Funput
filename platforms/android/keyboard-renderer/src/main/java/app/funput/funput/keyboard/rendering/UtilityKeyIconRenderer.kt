package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

internal class UtilityKeyIconRenderer(private val metrics: RenderMetrics) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val drawingRect = RectF()
    private val settingsPath = requireNotNull(PathParser.createPathFromPathData(SettingsPathData))
    private val settingsMatrix = Matrix()
    private val settingsViewport = RectF(0f, 0f, 24f, 24f)

    fun updateTheme(theme: KeyboardTheme) {
        paint.color = theme.labelColor
        paint.strokeWidth = metrics.dp(1.7f)
    }

    fun drawBackspace(canvas: Canvas, key: ResolvedKey) {
        val iconWidth = min(key.bounds.width * 0.46f, metrics.dp(25f))
        val iconHeight = iconWidth * 0.62f
        val left = key.bounds.centerX - iconWidth / 2f
        val right = key.bounds.centerX + iconWidth / 2f
        val top = key.bounds.centerY - iconHeight / 2f
        val bottom = key.bounds.centerY + iconHeight / 2f
        path.reset()
        path.moveTo(left, key.bounds.centerY)
        path.lineTo(left + iconHeight * 0.45f, top)
        path.lineTo(right, top)
        path.lineTo(right, bottom)
        path.lineTo(left + iconHeight * 0.45f, bottom)
        path.close()
        canvas.drawPath(path, paint)

        val crossCenter = right - iconHeight * 0.48f
        val crossRadius = iconHeight * 0.18f
        canvas.drawLine(
            crossCenter - crossRadius,
            key.bounds.centerY - crossRadius,
            crossCenter + crossRadius,
            key.bounds.centerY + crossRadius,
            paint,
        )
        canvas.drawLine(
            crossCenter + crossRadius,
            key.bounds.centerY - crossRadius,
            crossCenter - crossRadius,
            key.bounds.centerY + crossRadius,
            paint,
        )
    }

    fun drawSystemInputMethod(canvas: Canvas, key: ResolvedKey) {
        val radius = min(key.bounds.width, key.bounds.height) * 0.2f
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        drawingRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawOval(drawingRect, paint)
        canvas.drawOval(
            centerX - radius * 0.45f,
            centerY - radius,
            centerX + radius * 0.45f,
            centerY + radius,
            paint,
        )
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint)
    }

    fun drawSettings(canvas: Canvas, key: ResolvedKey) {
        val size = min(key.bounds.width, key.bounds.height) * 0.46f
        val half = size / 2f
        drawingRect.set(
            key.bounds.centerX - half,
            key.bounds.centerY - half,
            key.bounds.centerX + half,
            key.bounds.centerY + half,
        )
        settingsMatrix.setRectToRect(settingsViewport, drawingRect, Matrix.ScaleToFit.CENTER)
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        settingsPath.transform(settingsMatrix, path)
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        path.fillType = Path.FillType.WINDING
    }

    fun drawEmoji(canvas: Canvas, key: ResolvedKey) {
        val radius = min(key.bounds.width, key.bounds.height) * 0.19f
        drawingRect.set(
            key.bounds.centerX - radius,
            key.bounds.centerY - radius,
            key.bounds.centerX + radius,
            key.bounds.centerY + radius,
        )
        canvas.drawOval(drawingRect, paint)

        val eyeRadius = radius * 0.07f
        paint.style = Paint.Style.FILL
        canvas.drawCircle(
            key.bounds.centerX - radius * 0.34f,
            key.bounds.centerY - radius * 0.24f,
            eyeRadius,
            paint,
        )
        canvas.drawCircle(
            key.bounds.centerX + radius * 0.34f,
            key.bounds.centerY - radius * 0.24f,
            eyeRadius,
            paint,
        )
        paint.style = Paint.Style.STROKE
        drawingRect.set(
            key.bounds.centerX - radius * 0.48f,
            key.bounds.centerY - radius * 0.06f,
            key.bounds.centerX + radius * 0.48f,
            key.bounds.centerY + radius * 0.5f,
        )
        canvas.drawArc(drawingRect, 18f, 144f, false, paint)
    }

    private companion object {
        const val SettingsPathData =
            "M19.43,12.98c.04,-.32 .07,-.65 .07,-.98s-.03,-.66 -.07,-.98l2.11,-1.65" +
                "c.19,-.15 .24,-.42 .12,-.64l-2,-3.46c-.12,-.22 -.37,-.31 -.6,-.22l-2.49,1" +
                "c-.52,-.4 -1.08,-.73 -1.69,-.98L14.5,2.42C14.47,2.18 14.25,2 14,2h-4" +
                "c-.25,0 -.46,.18 -.5,.42L9.12,5.07c-.61,.25 -1.17,.58 -1.69,.98l-2.49,-1" +
                "c-.23,-.09 -.48,0 -.6,.22l-2,3.46c-.12,.22 -.07,.49 .12,.64l2.11,1.65" +
                "c-.04,.32 -.07,.65 -.07,.98s.03,.66 .07,.98l-2.11,1.65c-.19,.15 -.24,.42 -.12,.64" +
                "l2,3.46c.12,.22 .37,.31 .6,.22l2.49,-1c.52,.4 1.08,.73 1.69,.98l.38,2.65" +
                "c.04,.24 .25,.42 .5,.42h4c.25,0 .46,-.18 .5,-.42l.38,-2.65c.61,-.25 1.17,-.58 1.69,-.98" +
                "l2.49,1c.23,.09 .48,0 .6,-.22l2,-3.46c.12,-.22 .07,-.49 -.12,-.64z" +
                "M12,15.5A3.5,3.5 0,1 1,12,8a3.5,3.5 0,0 1,0,7.5z"
    }
}
