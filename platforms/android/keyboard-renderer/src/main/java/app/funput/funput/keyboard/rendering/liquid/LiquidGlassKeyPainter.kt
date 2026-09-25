package app.funput.funput.keyboard.rendering.liquid

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.funput.funput.keyboard.rendering.RenderMetrics
import app.funput.funput.theme.KeyboardTheme

/**
 * Turns a filled key plate into a pane of glass.
 *
 * Three passes, outward from the plate: a body ramp that lights the top and the base, a dark
 * contour that holds the silhouette against the ground, and a specular rim just inside it. The
 * contour and rim together read as the thickness of the pane, which is the part that makes the
 * key look refractive rather than merely translucent.
 *
 * Every shader is built in the unit square once per theme and mapped onto each key at draw time,
 * so a keyboard of sixty keys still only holds four gradients.
 */
internal class LiquidGlassKeyPainter(private val metrics: RenderMetrics) {
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val edgeRect = RectF()
    private val matrix = Matrix()
    private var body: Shader? = null
    private var pressedBody: Shader? = null
    private var rim: Shader? = null
    private var pressedRim: Shader? = null

    fun updateTheme(theme: KeyboardTheme) {
        contourPaint.strokeWidth = metrics.dp(ContourStrokeDp)
        contourPaint.color = LiquidGlassLighting.Contour
        rimPaint.strokeWidth = metrics.dp(RimStrokeDp)
        body = shader(LiquidGlassLighting.body(theme))
        pressedBody = shader(LiquidGlassLighting.pressedBody(theme))
        rim = shader(LiquidGlassLighting.rim(theme))
        pressedRim = shader(LiquidGlassLighting.pressedRim(theme))
    }

    fun draw(canvas: Canvas, rect: RectF, radius: Float, pressed: Boolean) {
        val fill = (if (pressed) pressedBody else body) ?: return
        val edge = (if (pressed) pressedRim else rim) ?: return
        if (!place(fill, rect)) return
        bodyPaint.shader = fill
        canvas.drawRoundRect(rect, radius, radius, bodyPaint)

        val contourInset = contourPaint.strokeWidth / 2f
        if (!inset(rect, contourInset)) return
        drawEdge(canvas, radius, contourInset, contourPaint)

        val rimInset = contourPaint.strokeWidth + rimPaint.strokeWidth / 2f
        if (!inset(rect, rimInset)) return
        if (!place(edge, edgeRect)) return
        rimPaint.shader = edge
        drawEdge(canvas, radius, rimInset, rimPaint)
    }

    private fun drawEdge(canvas: Canvas, radius: Float, inset: Float, paint: Paint) {
        val edgeRadius = (radius - inset).coerceAtLeast(0f)
        canvas.drawRoundRect(edgeRect, edgeRadius, edgeRadius, paint)
    }

    /** Shrinks [edgeRect] onto the stroke's own centre line, reporting whether anything is left. */
    private fun inset(rect: RectF, amount: Float): Boolean {
        edgeRect.set(rect)
        edgeRect.inset(amount, amount)
        return edgeRect.width() > 0f && edgeRect.height() > 0f
    }

    private fun shader(ramp: LiquidGlassRamp?): Shader? {
        ramp ?: return null
        return LinearGradient(
            0f,
            0f,
            ramp.axis.endX,
            ramp.axis.endY,
            ramp.colors,
            ramp.positions,
            Shader.TileMode.CLAMP,
        )
    }

    private fun place(shader: Shader, rect: RectF): Boolean {
        if (rect.width() <= 0f || rect.height() <= 0f) return false
        matrix.setScale(rect.width(), rect.height())
        matrix.postTranslate(rect.left, rect.top)
        shader.setLocalMatrix(matrix)
        return true
    }

    private companion object {
        const val ContourStrokeDp = 0.5f
        const val RimStrokeDp = 0.8f
    }
}
