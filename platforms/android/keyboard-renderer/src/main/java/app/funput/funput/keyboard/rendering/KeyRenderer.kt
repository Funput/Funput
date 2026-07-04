package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme

/** Draws key surfaces and text while delegating symbolic key artwork. */
internal class KeyRenderer(private val metrics: RenderMetrics) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val drawingRect = RectF()
    private val fontMetrics = Paint.FontMetrics()
    private val characterTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val specialTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val iconRenderer = KeyIconRenderer(metrics)
    private val spacebarRenderer = SpacebarContentRenderer(metrics)

    fun updateTheme(theme: KeyboardTheme) {
        shadowPaint.color = theme.keyShadowColor
        borderPaint.strokeWidth = metrics.dp(theme.keyBorderWidthDp)
        iconRenderer.updateTheme(theme)
    }

    fun draw(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        isPressed: Boolean,
        shiftState: ShiftState,
        language: KeyboardLanguage,
    ) {
        val isActivated = key.spec.role == KeyRole.SHIFT && shiftState.isActive
        drawSurface(canvas, key, theme, isPressed, isActivated)
        when {
            key.spec.role == KeyRole.SPACE -> spacebarRenderer.draw(canvas, key, theme, language)
            !iconRenderer.draw(canvas, key, shiftState) -> drawLabels(canvas, key, theme, shiftState)
        }
    }

    private fun drawSurface(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        isPressed: Boolean,
        isActivated: Boolean,
    ) {
        val bounds = key.bounds
        val radius = metrics.dp(theme.keyCornerRadiusDp)
        val shadowOffset = metrics.dp(
            if (isPressed) theme.pressedKeyShadowOffsetDp else theme.keyShadowOffsetDp,
        )
        drawingRect.set(bounds.left, bounds.top + shadowOffset, bounds.right, bounds.bottom + shadowOffset)
        canvas.drawRoundRect(drawingRect, radius, radius, shadowPaint)

        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        fillPaint.color = when {
            isPressed -> theme.pressedKeyColor
            isActivated -> theme.activatedKeyColor
            key.spec.role.isSpecial -> theme.specialKeyColor
            else -> theme.keyColor
        }
        borderPaint.color = when {
            isPressed -> theme.pressedKeyBorderColor
            isActivated -> theme.activatedKeyBorderColor
            else -> theme.keyBorderColor
        }
        canvas.drawRoundRect(drawingRect, radius, radius, fillPaint)
        canvas.drawRoundRect(drawingRect, radius, radius, borderPaint)
    }

    private fun drawLabels(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        shiftState: ShiftState,
    ) {
        val role = key.spec.role
        labelPaint.color = theme.labelColor
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
            KeyRole.CHARACTER, KeyRole.VNI_MODIFIER, KeyRole.PUNCTUATION -> CharacterLabelSizeSp
            else -> SpecialLabelSizeSp
        }

    private val KeyRole.isSpecial: Boolean
        get() = this == KeyRole.SHIFT || this == KeyRole.BACKSPACE || this == KeyRole.SYMBOLS ||
            this == KeyRole.EMOJI || this == KeyRole.ENTER

    private companion object {
        const val CharacterLabelSizeSp = 20f
        const val SpecialLabelSizeSp = 13f
        const val SecondaryLabelSizeSp = 9f
    }
}
