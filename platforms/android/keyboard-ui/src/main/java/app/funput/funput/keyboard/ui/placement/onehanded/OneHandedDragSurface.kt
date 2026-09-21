package app.funput.funput.keyboard.ui.placement.onehanded

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.OneHandedSide
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** Draws the one-handed frame and turns inner-edge dragging into width previews. */
internal class OneHandedDragSurface(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AdjustmentBlue
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val handles = Paint(frame).apply { strokeWidth = dp(4f) }
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AdjustmentBlue
        textAlign = Paint.Align.CENTER
        textSize = dp(28f)
    }
    private var scrimColor = Color.argb(152, 240, 242, 246)
    private var widthFraction = KeyboardPlacementPreferences.DefaultOneHandedWidthFraction
    private var side = OneHandedSide.RIGHT
    private var dragging = false

    var onWidthPreview: (Float) -> Unit = {}
    var onWidthSettled: (Float) -> Unit = {}

    init { isClickable = true; updateDescription() }

    fun render(fraction: Float, alignment: OneHandedSide) {
        if (!dragging) widthFraction = fraction
        side = alignment
        updateDescription()
        invalidate()
    }

    fun updateTheme(theme: KeyboardTheme) {
        val palette = KeyboardPanelPalette.from(theme)
        scrimColor = palette.backgroundEnd.withAlpha(168)
        frame.color = palette.accent
        handles.color = palette.accent
        arrow.color = palette.accent
        invalidate()
    }

    fun contentBounds(): RectF {
        val contentWidth = width * widthFraction
        val left = if (side == OneHandedSide.RIGHT) width - contentWidth else 0f
        return RectF(left, 0f, left + contentWidth, height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(scrimColor)
        val bounds = contentBounds().apply { inset(dp(4f), dp(4f)) }
        canvas.drawRect(bounds, frame)
        drawCorners(canvas, bounds)
        val edge = if (side == OneHandedSide.RIGHT) bounds.left else bounds.right
        canvas.drawText("↔", edge, bounds.centerY() + dp(9f), arrow)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            val bounds = contentBounds()
            val edge = if (side == OneHandedSide.RIGHT) bounds.left else bounds.right
            dragging = abs(event.x - edge) <= dp(36f)
            if (dragging) parent.requestDisallowInterceptTouchEvent(true)
            true
        }
        MotionEvent.ACTION_MOVE -> {
            if (dragging && width > 0) {
                val raw = if (side == OneHandedSide.LEFT) event.x / width else (width - event.x) / width
                widthFraction = raw.coerceIn(
                    KeyboardPlacementPreferences.MinOneHandedWidthFraction,
                    KeyboardPlacementPreferences.MaxOneHandedWidthFraction,
                )
                onWidthPreview(widthFraction)
                updateDescription()
                invalidate()
            }
            true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            if (dragging) onWidthSettled(widthFraction)
            dragging = false
            parent.requestDisallowInterceptTouchEvent(false)
            performClick()
            true
        }
        else -> true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun drawCorners(canvas: Canvas, bounds: RectF) {
        val length = dp(18f)
        fun corner(x: Float, y: Float, xd: Float, yd: Float) {
            canvas.drawLine(x, y, x + length * xd, y, handles)
            canvas.drawLine(x, y, x, y + length * yd, handles)
        }
        corner(bounds.left, bounds.top, 1f, 1f)
        corner(bounds.right, bounds.top, -1f, 1f)
        corner(bounds.left, bounds.bottom, 1f, -1f)
        corner(bounds.right, bounds.bottom, -1f, -1f)
    }

    private fun updateDescription() {
        contentDescription = resources.getString(
            R.string.placement_one_handed_drag,
            (widthFraction * 100).roundToInt(),
        )
    }

    private fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)
    private fun dp(value: Float) = value * density

    private companion object { const val AdjustmentBlue = -0xF28701 }
}
