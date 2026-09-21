package app.funput.funput.keyboard.ui.placement

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Full-keyboard direct-manipulation overlay used while changing elevation. */
internal class KeyboardPlacementEditorView(context: Context) : FrameLayout(context) {
    private val density = resources.displayMetrics.density
    private val dragSurface = KeyboardPlacementDragSurface(context)
    private val done = Button(context).apply {
        text = "✓  ${resources.getString(R.string.placement_done).uppercase()}"
        contentDescription = resources.getString(R.string.placement_done)
        isAllCaps = false
        textSize = 15f
        setTextColor(Color.WHITE)
        elevation = dp(6).toFloat()
        background = pill(AdjustmentBlue)
    }

    var onOffsetPreview: (Float) -> Unit
        get() = dragSurface.onOffsetPreview
        set(value) { dragSurface.onOffsetPreview = value }
    var onOffsetSettled: (Float) -> Unit
        get() = dragSurface.onOffsetSettled
        set(value) { dragSurface.onOffsetSettled = value }
    var onDone: () -> Unit = {}

    init {
        isClickable = true
        addView(dragSurface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(done, LayoutParams(dp(132), dp(52), Gravity.CENTER))
        done.setOnClickListener { onDone() }
        visibility = View.GONE
    }

    fun render(maximumOffsetPx: Int, appliedOffsetPx: Int) {
        dragSurface.render(maximumOffsetPx / density, appliedOffsetPx / density)
    }

    fun updateTheme(theme: KeyboardTheme) {
        dragSurface.updateTheme(theme)
        val palette = KeyboardPanelPalette.from(theme)
        done.background = pill(palette.accent)
        done.setTextColor(palette.readableOn(palette.accent, Color.WHITE))
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(26).toFloat()
        setColor(color)
    }

    private fun dp(value: Int) = (value * density).roundToInt()

    private companion object {
        const val AdjustmentBlue = -0xF28701
    }
}
