package app.funput.funput.keyboard.ui.placement.onehanded

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import app.funput.funput.keyboard.placement.OneHandedSide
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Full-keyboard direct editor for one-handed width and physical side. */
internal class OneHandedPlacementEditorView(context: Context) : FrameLayout(context) {
    private val density = resources.displayMetrics.density
    private val dragSurface = OneHandedDragSurface(context)
    private val done = actionButton("✓  ${resources.getString(R.string.placement_done).uppercase()}")
    private val switchSide = actionButton("⇆")
    private var side = OneHandedSide.RIGHT

    var onWidthPreview: (Float) -> Unit
        get() = dragSurface.onWidthPreview
        set(value) { dragSurface.onWidthPreview = value }
    var onWidthSettled: (Float) -> Unit
        get() = dragSurface.onWidthSettled
        set(value) { dragSurface.onWidthSettled = value }
    var onSideToggle: () -> Unit = {}
    var onDone: () -> Unit = {}

    init {
        isClickable = true
        addView(dragSurface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(done, LayoutParams(dp(132), dp(52)))
        addView(switchSide, LayoutParams(dp(40), dp(48)))
        done.contentDescription = resources.getString(R.string.placement_done)
        done.setOnClickListener { onDone() }
        switchSide.setOnClickListener { onSideToggle() }
        doOnLayout { positionControls() }
        visibility = View.GONE
    }

    fun render(widthFraction: Float, alignment: OneHandedSide) {
        side = alignment
        dragSurface.render(widthFraction, alignment)
        switchSide.contentDescription = resources.getString(
            if (side == OneHandedSide.RIGHT) R.string.placement_move_left
            else R.string.placement_move_right,
        )
        post(::positionControls)
    }

    fun updateTheme(theme: KeyboardTheme) {
        dragSurface.updateTheme(theme)
        val palette = KeyboardPanelPalette.from(theme)
        listOf(done, switchSide).forEach {
            it.background = pill(palette.accent)
            it.setTextColor(palette.readableOn(palette.accent, Color.WHITE))
        }
    }

    private fun positionControls() {
        if (width == 0) return
        val bounds = dragSurface.contentBounds()
        done.x = bounds.centerX() - done.width / 2f
        done.y = bounds.centerY() - done.height / 2f
        val gutterCenter = if (side == OneHandedSide.RIGHT) bounds.left / 2f
        else bounds.right + (width - bounds.right) / 2f
        switchSide.x = (gutterCenter - switchSide.width / 2f).coerceIn(0f, width - switchSide.width.toFloat())
        switchSide.y = bounds.centerY() - switchSide.height / 2f
    }

    private fun actionButton(label: String) = Button(context).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setTextColor(Color.WHITE)
        elevation = dp(6).toFloat()
        background = pill(AdjustmentBlue)
    }

    private fun pill(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(26).toFloat()
        setColor(color)
    }

    private fun dp(value: Int) = (value * density).roundToInt()
    private companion object { const val AdjustmentBlue = -0xF28701 }
}
