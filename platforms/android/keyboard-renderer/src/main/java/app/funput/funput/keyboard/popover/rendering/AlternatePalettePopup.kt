package app.funput.funput.keyboard.popover.rendering

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.popover.interaction.AlternateSelectionPreview
import app.funput.funput.keyboard.rendering.RenderMetrics
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.ceil
import kotlin.math.floor

/** Draws an alternate palette above the IME without changing the input view's height. */
internal class AlternatePalettePopup(private val host: View) {
    private val padding = ceil(host.resources.displayMetrics.density * ShadowPaddingDp).toInt()
    private val content = PaletteView(host.context)
    private val popup = PopupWindow(content).apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL)
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        isAttachedInDecor = false
        isClippingEnabled = false
        isFocusable = false
        isTouchable = false
        animationStyle = 0
    }

    fun render(
        preview: AlternateSelectionPreview?,
        theme: KeyboardTheme,
        shiftState: ShiftState,
    ) {
        if (preview == null || !host.isAttachedToWindow || host.windowToken == null) {
            dismiss()
            return
        }
        content.render(preview, theme, shiftState, padding.toFloat())
        val bounds = preview.layout.bounds
        val width = ceil(bounds.width.toDouble()).toInt() + padding * 2
        val height = ceil(bounds.height.toDouble()).toInt() + padding * 2
        val xOffset = floor(bounds.left.toDouble()).toInt() - padding
        val yOffset = floor(bounds.top.toDouble()).toInt() - padding - host.height
        if (popup.isShowing) {
            popup.update(host, xOffset, yOffset, width, height)
            return
        }
        popup.width = width
        popup.height = height
        try {
            popup.showAsDropDown(host, xOffset, yOffset, Gravity.START)
        } catch (_: WindowManager.BadTokenException) {
            dismiss()
        }
    }

    fun dismiss() {
        if (popup.isShowing) popup.dismiss()
    }

    private class PaletteView(context: Context) : View(context) {
        private val renderer = AlternatePaletteRenderer(RenderMetrics(resources))
        private var preview: AlternateSelectionPreview? = null
        private lateinit var theme: KeyboardTheme
        private var shiftState = ShiftState.OFF
        private var padding = 0f

        fun render(
            preview: AlternateSelectionPreview,
            theme: KeyboardTheme,
            shiftState: ShiftState,
            padding: Float,
        ) {
            this.preview = preview
            this.theme = theme
            this.shiftState = shiftState
            this.padding = padding
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val preview = preview ?: return
            val bounds = preview.layout.bounds
            val checkpoint = canvas.save()
            canvas.translate(padding - bounds.left, padding - bounds.top)
            renderer.draw(canvas, preview, theme, shiftState)
            canvas.restoreToCount(checkpoint)
        }
    }

    private companion object {
        const val ShadowPaddingDp = 4f
    }
}
