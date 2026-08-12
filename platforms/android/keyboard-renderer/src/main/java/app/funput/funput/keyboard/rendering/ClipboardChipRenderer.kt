package app.funput.funput.keyboard.rendering

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.R
import app.funput.funput.keyboard.interaction.ClipboardTargetId
import app.funput.funput.keyboard.interaction.PressedKeyState
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.title
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.LocalKeyboardThemeCatalog

internal class ClipboardChipRenderer(
    private val resources: Resources,
    private val metrics: RenderMetrics,
) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var theme: KeyboardTheme = LocalKeyboardThemeCatalog.defaultTheme.theme

    fun updateTheme(value: KeyboardTheme) { theme = value }

    fun draw(
        canvas: Canvas,
        bounds: KeyBounds,
        hint: KeyboardClipboardHint,
        pressed: PressedKeyState,
    ) {
        drawPressedState(canvas, bounds, pressed)
        val inset = metrics.dp(3f)
        val capsuleRight = minOf(bounds.right, bounds.left + metrics.dp(CapsuleWidthDp))
        rect.set(bounds.left, bounds.top + inset, capsuleRight, bounds.bottom - inset)
        fill.color = theme.accentColor
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, fill)
        drawPasteLabel(canvas, rect)
        drawHint(canvas, bounds, capsuleRight, hint)
    }

    private fun drawPressedState(canvas: Canvas, bounds: KeyBounds, pressed: PressedKeyState) {
        if (!pressed.isPressed(ClipboardTargetId)) return
        rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        fill.color = theme.pressedKeyColor
        canvas.drawRoundRect(rect, metrics.dp(theme.keyCornerRadiusDp), metrics.dp(theme.keyCornerRadiusDp), fill)
    }

    private fun drawPasteLabel(canvas: Canvas, bounds: RectF) {
        configureText(14f, Typeface.DEFAULT_BOLD, clipboardForeground(theme.accentColor))
        canvas.drawText(
            resources.getString(R.string.clipboard_paste),
            bounds.centerX(),
            centeredBaseline(bounds.centerY()),
            text,
        )
    }

    private fun drawHint(
        canvas: Canvas,
        bounds: KeyBounds,
        capsuleRight: Float,
        hint: KeyboardClipboardHint,
    ) {
        val left = capsuleRight + metrics.dp(HintGapDp)
        val available = bounds.right - left
        if (available < metrics.dp(MinimumHintWidthDp)) return
        configureText(13f, Typeface.DEFAULT, theme.secondaryLabelColor)
        text.textAlign = Paint.Align.LEFT
        val label = TextUtils.ellipsize(
            hint.title(resources), text, available, TextUtils.TruncateAt.END,
        ).toString()
        canvas.drawText(label, left, centeredBaseline(bounds.centerY), text)
    }

    private fun configureText(sizeSp: Float, face: Typeface, color: Int) {
        text.textSize = metrics.sp(sizeSp)
        text.typeface = face
        text.color = color
        text.textAlign = Paint.Align.CENTER
    }

    private fun centeredBaseline(centerY: Float): Float =
        centerY - (text.fontMetrics.ascent + text.fontMetrics.descent) / 2f

    private companion object {
        const val CapsuleWidthDp = 52f
        const val HintGapDp = 8f
        const val MinimumHintWidthDp = 28f
    }
}

internal fun clipboardForeground(background: Int): Int =
    if (clipboardLuminance(background) > 0.179) Color.BLACK else Color.WHITE

internal fun clipboardContrast(foreground: Int, background: Int): Double {
    val lighter = maxOf(clipboardLuminance(foreground), clipboardLuminance(background))
    val darker = minOf(clipboardLuminance(foreground), clipboardLuminance(background))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun clipboardLuminance(color: Int): Double {
    fun channel(shift: Int): Double {
        val value = ((color shr shift) and 0xff) / 255.0
        return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }
    return channel(16) * 0.2126 + channel(8) * 0.7152 + channel(0) * 0.0722
}
