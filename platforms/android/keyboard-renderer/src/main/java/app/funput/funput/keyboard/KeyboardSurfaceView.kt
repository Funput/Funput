package app.funput.funput.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import app.funput.funput.keyboard.layout.KeyboardGeometry
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.KeyboardLayouts
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Low-allocation Canvas renderer shared by the future IME and the preview application.
 *
 * Input behavior intentionally does not live here. This view owns presentation and resolved
 * geometry only; touch state and IME actions will be layered on top of the same key model.
 */
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    var inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX
        set(value) {
            if (field == value) return
            field = value
            keyboardLayout = KeyboardLayouts.forInputMethod(value)
            requestLayout()
            resolveGeometry()
            invalidate()
        }

    var keyboardTheme: KeyboardTheme = KeyboardTheme.Aurora
        set(value) {
            if (field == value) return
            field = value
            applyTheme()
            rebuildBackgroundShader()
            invalidate()
        }

    var suggestions: List<String> = emptyList()
        set(value) {
            val normalized = value
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .take(MaxVisibleSuggestions)
                .toList()
            if (field == normalized) return
            field = normalized
            invalidate()
        }

    private var keyboardLayout: KeyboardLayout = KeyboardLayouts.forInputMethod(inputMethod)
    private var resolvedKeyboard: ResolvedKeyboard? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val drawingRect = RectF()
    private val iconRect = RectF()
    private val iconPath = Path()
    private val fontMetrics = Paint.FontMetrics()
    private val characterTypeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val specialTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private val density = resources.displayMetrics.density

    init {
        applyTheme()
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dpToPx(DefaultKeyboardWidthDp).roundToInt()
        val desiredHeight = dpToPx(recommendedHeightDp(inputMethod)).roundToInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resolveGeometry()
        rebuildBackgroundShader()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val keyboard = resolvedKeyboard ?: return
        drawSuggestionBar(canvas, keyboard.suggestionBar)
        keyboard.keys.forEach { key ->
            drawKeyBackground(canvas, key)
            drawKeyContent(canvas, key)
        }
    }

    private fun resolveGeometry() {
        if (width <= 0 || height <= 0) return
        resolvedKeyboard = KeyboardGeometry.resolve(
            layout = keyboardLayout,
            width = width.toFloat(),
            height = height.toFloat(),
            spec = KeyboardGeometrySpec.fromDensity(density),
        )
    }

    private fun applyTheme() {
        val theme = keyboardTheme
        keyShadowPaint.color = theme.keyShadowColor
        keyBorderPaint.color = theme.keyBorderColor
        keyBorderPaint.strokeWidth = dpToPx(theme.keyBorderWidthDp)
        labelPaint.color = theme.labelColor
        iconPaint.color = theme.labelColor
        iconPaint.strokeWidth = dpToPx(1.7f)
    }

    private fun rebuildBackgroundShader() {
        if (width <= 0 || height <= 0) return
        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            keyboardTheme.backgroundStartColor,
            keyboardTheme.backgroundEndColor,
            Shader.TileMode.CLAMP,
        )
    }

    private fun drawKeyBackground(canvas: Canvas, key: ResolvedKey) {
        val bounds = key.bounds
        val radius = dpToPx(keyboardTheme.keyCornerRadiusDp)
        val shadowOffset = dpToPx(keyboardTheme.keyShadowOffsetDp)

        drawingRect.set(bounds.left, bounds.top + shadowOffset, bounds.right, bounds.bottom + shadowOffset)
        canvas.drawRoundRect(drawingRect, radius, radius, keyShadowPaint)

        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        keyPaint.color = if (key.spec.role.isSpecial) {
            keyboardTheme.specialKeyColor
        } else {
            keyboardTheme.keyColor
        }
        canvas.drawRoundRect(drawingRect, radius, radius, keyPaint)
        canvas.drawRoundRect(drawingRect, radius, radius, keyBorderPaint)
    }

    private fun drawSuggestionBar(canvas: Canvas, suggestionBar: ResolvedSuggestionBar) {
        val bounds = suggestionBar.suggestionsBounds
        val radius = dpToPx(keyboardTheme.keyCornerRadiusDp)

        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        keyPaint.color = keyboardTheme.keyColor
        canvas.drawRoundRect(drawingRect, radius, radius, keyPaint)
        canvas.drawRoundRect(drawingRect, radius, radius, keyBorderPaint)

        if (suggestions.isEmpty()) return

        labelPaint.color = keyboardTheme.labelColor
        labelPaint.textSize = spToPx(SuggestionLabelSizeSp)
        labelPaint.typeface = specialTypeface
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.getFontMetrics(fontMetrics)

        val segmentWidth = bounds.width / suggestions.size
        val baseline = bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        suggestions.forEachIndexed { index, suggestion ->
            if (index > 0) {
                val dividerX = bounds.left + segmentWidth * index
                canvas.drawLine(
                    dividerX,
                    bounds.top + dpToPx(9f),
                    dividerX,
                    bounds.bottom - dpToPx(9f),
                    keyBorderPaint,
                )
            }
            canvas.drawText(
                suggestion,
                bounds.left + segmentWidth * (index + 0.5f),
                baseline,
                labelPaint,
            )
        }
    }

    private fun drawKeyContent(canvas: Canvas, key: ResolvedKey) {
        when (key.spec.role) {
            KeyRole.SHIFT -> drawShiftIcon(canvas, key)
            KeyRole.BACKSPACE -> drawBackspaceIcon(canvas, key)
            KeyRole.EMOJI -> drawEmojiIcon(canvas, key)
            KeyRole.ENTER -> drawEnterIcon(canvas, key)
            else -> drawLabels(canvas, key)
        }
    }

    private fun drawLabels(canvas: Canvas, key: ResolvedKey) {
        val role = key.spec.role
        labelPaint.color = when (role) {
            KeyRole.SPACE -> keyboardTheme.secondaryLabelColor
            else -> keyboardTheme.labelColor
        }
        labelPaint.textSize = spToPx(
            when (role) {
                KeyRole.CHARACTER, KeyRole.VNI_MODIFIER, KeyRole.PUNCTUATION -> CharacterLabelSizeSp
                KeyRole.SPACE -> SpaceLabelSizeSp
                else -> SpecialLabelSizeSp
            },
        )
        labelPaint.typeface = if (role == KeyRole.CHARACTER) characterTypeface else specialTypeface
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.getFontMetrics(fontMetrics)

        val baseline = key.bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(key.spec.label, key.bounds.centerX, baseline, labelPaint)

        val secondaryLabel = key.spec.secondaryLabel ?: return
        labelPaint.color = keyboardTheme.accentColor
        labelPaint.textSize = spToPx(SecondaryLabelSizeSp)
        labelPaint.textAlign = Paint.Align.RIGHT
        labelPaint.getFontMetrics(fontMetrics)
        canvas.drawText(
            secondaryLabel,
            key.bounds.right - dpToPx(6f),
            key.bounds.top + dpToPx(5f) - fontMetrics.ascent,
            labelPaint,
        )
    }

    private fun drawShiftIcon(canvas: Canvas, key: ResolvedKey) {
        val size = min(key.bounds.width, key.bounds.height) * 0.38f
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        val half = size / 2f

        iconPath.reset()
        iconPath.moveTo(centerX, centerY - half)
        iconPath.lineTo(centerX + half, centerY)
        iconPath.lineTo(centerX + half * 0.42f, centerY)
        iconPath.lineTo(centerX + half * 0.42f, centerY + half)
        iconPath.lineTo(centerX - half * 0.42f, centerY + half)
        iconPath.lineTo(centerX - half * 0.42f, centerY)
        iconPath.lineTo(centerX - half, centerY)
        iconPath.close()
        canvas.drawPath(iconPath, iconPaint)
    }

    private fun drawBackspaceIcon(canvas: Canvas, key: ResolvedKey) {
        val iconWidth = min(key.bounds.width * 0.46f, dpToPx(25f))
        val iconHeight = iconWidth * 0.62f
        val left = key.bounds.centerX - iconWidth / 2f
        val right = key.bounds.centerX + iconWidth / 2f
        val top = key.bounds.centerY - iconHeight / 2f
        val bottom = key.bounds.centerY + iconHeight / 2f

        iconPath.reset()
        iconPath.moveTo(left, key.bounds.centerY)
        iconPath.lineTo(left + iconHeight * 0.45f, top)
        iconPath.lineTo(right, top)
        iconPath.lineTo(right, bottom)
        iconPath.lineTo(left + iconHeight * 0.45f, bottom)
        iconPath.close()
        canvas.drawPath(iconPath, iconPaint)

        val crossRadius = iconHeight * 0.18f
        canvas.drawLine(
            right - iconHeight * 0.48f - crossRadius,
            key.bounds.centerY - crossRadius,
            right - iconHeight * 0.48f + crossRadius,
            key.bounds.centerY + crossRadius,
            iconPaint,
        )
        canvas.drawLine(
            right - iconHeight * 0.48f + crossRadius,
            key.bounds.centerY - crossRadius,
            right - iconHeight * 0.48f - crossRadius,
            key.bounds.centerY + crossRadius,
            iconPaint,
        )
    }

    private fun drawEmojiIcon(canvas: Canvas, key: ResolvedKey) {
        val radius = min(key.bounds.width, key.bounds.height) * 0.19f
        iconRect.set(
            key.bounds.centerX - radius,
            key.bounds.centerY - radius,
            key.bounds.centerX + radius,
            key.bounds.centerY + radius,
        )
        canvas.drawOval(iconRect, iconPaint)

        val eyeRadius = radius * 0.07f
        iconPaint.style = Paint.Style.FILL
        canvas.drawCircle(
            key.bounds.centerX - radius * 0.34f,
            key.bounds.centerY - radius * 0.24f,
            eyeRadius,
            iconPaint,
        )
        canvas.drawCircle(
            key.bounds.centerX + radius * 0.34f,
            key.bounds.centerY - radius * 0.24f,
            eyeRadius,
            iconPaint,
        )
        iconPaint.style = Paint.Style.STROKE

        iconRect.set(
            key.bounds.centerX - radius * 0.48f,
            key.bounds.centerY - radius * 0.06f,
            key.bounds.centerX + radius * 0.48f,
            key.bounds.centerY + radius * 0.5f,
        )
        canvas.drawArc(iconRect, 18f, 144f, false, iconPaint)
    }

    private fun drawEnterIcon(canvas: Canvas, key: ResolvedKey) {
        val size = min(key.bounds.width, key.bounds.height) * 0.34f
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        val right = centerX + size * 0.5f
        val left = centerX - size * 0.5f

        iconPath.reset()
        iconPath.moveTo(right, centerY - size * 0.48f)
        iconPath.lineTo(right, centerY + size * 0.18f)
        iconPath.quadTo(right, centerY + size * 0.45f, right - size * 0.28f, centerY + size * 0.45f)
        iconPath.lineTo(left, centerY + size * 0.45f)
        iconPath.moveTo(left, centerY + size * 0.45f)
        iconPath.lineTo(left + size * 0.3f, centerY + size * 0.15f)
        iconPath.moveTo(left, centerY + size * 0.45f)
        iconPath.lineTo(left + size * 0.3f, centerY + size * 0.75f)
        canvas.drawPath(iconPath, iconPaint)
    }

    private fun dpToPx(dp: Float): Float = dp * density

    private fun spToPx(sp: Float): Float = sp * density * resources.configuration.fontScale.coerceAtMost(MaxFontScale)

    private val KeyRole.isSpecial: Boolean
        get() = when (this) {
            KeyRole.SHIFT,
            KeyRole.BACKSPACE,
            KeyRole.SYMBOLS,
            KeyRole.EMOJI,
            KeyRole.ENTER,
            -> true

            else -> false
        }

    companion object {
        private const val DefaultKeyboardWidthDp = 360f
        private const val CharacterLabelSizeSp = 20f
        private const val SpecialLabelSizeSp = 13f
        private const val SpaceLabelSizeSp = 12f
        private const val SecondaryLabelSizeSp = 9f
        private const val SuggestionLabelSizeSp = 14f
        private const val MaxFontScale = 1.25f
        private const val MaxVisibleSuggestions = 3

        fun recommendedHeightDp(inputMethod: KeyboardInputMethod): Float = when (inputMethod) {
            KeyboardInputMethod.TELEX -> 290f
            KeyboardInputMethod.VNI -> 348f
        }
    }
}
