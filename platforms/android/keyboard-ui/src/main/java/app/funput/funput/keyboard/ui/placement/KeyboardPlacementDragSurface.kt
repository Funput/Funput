package app.funput.funput.keyboard.ui.placement

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Draws the resize frame and converts vertical dragging into an elevation preview. */
internal class KeyboardPlacementDragSurface(context: Context) : View(context) {
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
        textSize = dp(30f)
    }
    private var scrimColor = Color.argb(152, 240, 242, 246)
    private var maximumOffsetDp = 0f
    private var currentOffsetDp = 0f
    private var dragStartRawY = 0f
    private var dragStartOffsetDp = 0f
    private var dragging = false

    var onOffsetPreview: (Float) -> Unit = {}
    var onOffsetSettled: (Float) -> Unit = {}

    init {
        isClickable = true
        updateDescription()
    }

    fun render(maximumDp: Float, appliedDp: Float) {
        maximumOffsetDp = maximumDp.coerceAtLeast(0f)
        if (!dragging) currentOffsetDp = appliedDp.coerceIn(0f, maximumOffsetDp)
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

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(scrimColor)
        val inset = dp(4f)
        val bounds = RectF(inset, inset, width - inset, height - inset)
        canvas.drawRect(bounds, frame)
        drawCorners(canvas, bounds)
        canvas.drawText("↕", bounds.right - dp(24f), bounds.centerY() + dp(10f), arrow)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            dragging = true
            dragStartRawY = event.rawY
            dragStartOffsetDp = currentOffsetDp
            parent.requestDisallowInterceptTouchEvent(true)
            true
        }
        MotionEvent.ACTION_MOVE -> {
            val deltaDp = (dragStartRawY - event.rawY) / density
            currentOffsetDp = (dragStartOffsetDp + deltaDp).coerceIn(0f, maximumOffsetDp)
            onOffsetPreview(currentOffsetDp)
            updateDescription()
            true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            dragging = false
            parent.requestDisallowInterceptTouchEvent(false)
            onOffsetSettled(currentOffsetDp)
            performClick()
            true
        }
        else -> true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawCorners(canvas: Canvas, bounds: RectF) {
        val length = dp(18f)
        fun corner(x: Float, y: Float, xDirection: Float, yDirection: Float) {
            canvas.drawLine(x, y, x + length * xDirection, y, handles)
            canvas.drawLine(x, y, x, y + length * yDirection, handles)
        }
        corner(bounds.left, bounds.top, 1f, 1f)
        corner(bounds.right, bounds.top, -1f, 1f)
        corner(bounds.left, bounds.bottom, 1f, -1f)
        corner(bounds.right, bounds.bottom, -1f, -1f)
    }

    private fun updateDescription() {
        contentDescription = if (maximumOffsetDp > 0f) {
            resources.getString(R.string.placement_drag_description, currentOffsetDp.roundToInt())
        } else {
            resources.getString(R.string.placement_unavailable)
        }
    }

    private fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)
    private fun dp(value: Float) = value * density

    private companion object {
        const val AdjustmentBlue = -0xF28701
    }
}
