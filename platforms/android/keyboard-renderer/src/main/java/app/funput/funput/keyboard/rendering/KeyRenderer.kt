package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme

/** Draws key surfaces and text while delegating symbolic key artwork. */
internal class KeyRenderer(private val metrics: RenderMetrics) {
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val fontMetrics = Paint.FontMetrics()
    private val characterTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val specialTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val surfacePainter = KeySurfacePainter(metrics)
    private val iconRenderer = KeyIconRenderer(metrics)
    private val spacebarRenderer = SpacebarContentRenderer(metrics)

    fun updateTheme(theme: KeyboardTheme) {
        surfacePainter.updateTheme(theme)
        iconRenderer.updateTheme(theme)
    }

    fun draw(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        isPressed: Boolean,
        shiftState: ShiftState,
        language: KeyboardLanguage,
        enterAction: KeyboardEnterAction,
    ) {
        if (key.spec.role == KeyRole.PLACEHOLDER) return
        val isActivated = key.spec.role == KeyRole.SHIFT && shiftState.isActive
        surfacePainter.draw(canvas, key, theme, isPressed, isActivated)
        when {
            key.spec.role == KeyRole.SPACE -> spacebarRenderer.draw(canvas, key, theme, language)
            !iconRenderer.draw(canvas, key, shiftState, enterAction) ->
                drawLabels(canvas, key, theme, shiftState)
        }
    }

    private fun drawLabels(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        shiftState: ShiftState,
    ) {
        val role = key.spec.role
        labelPaint.color = if (role.isSpecial) theme.specialLabelColor else theme.labelColor
        labelPaint.textSize = metrics.sp(role.labelSizeSp)
        labelPaint.typeface = if (role == KeyRole.CHARACTER) characterTypeface else specialTypeface
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.getFontMetrics(fontMetrics)
        val baseline = key.bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        val label = if (role == KeyRole.CHARACTER && shiftState.isActive) {
            key.spec.shiftedLabel ?: key.spec.label
        } else {
            key.spec.label
        }
        canvas.drawText(label, key.bounds.centerX, baseline, labelPaint)

        val secondaryLabel = key.spec.secondaryLabel ?: return
        labelPaint.color = theme.accentColor
        labelPaint.textSize = metrics.sp(SecondaryLabelSizeSp)
        labelPaint.textAlign = Paint.Align.RIGHT
        labelPaint.getFontMetrics(fontMetrics)
        canvas.drawText(
            secondaryLabel,
            key.bounds.right - metrics.dp(6f),
            key.bounds.top + metrics.dp(5f) - fontMetrics.ascent,
            labelPaint,
        )
    }

    private val KeyRole.labelSizeSp: Float
        get() = when (this) {
            KeyRole.PLACEHOLDER,
            KeyRole.CHARACTER, KeyRole.VNI_MODIFIER, KeyRole.PUNCTUATION -> CharacterLabelSizeSp
            else -> SpecialLabelSizeSp
        }

    private companion object {
        const val CharacterLabelSizeSp = 20f
        const val SpecialLabelSizeSp = 13f
        const val SecondaryLabelSizeSp = 9f
    }
}
