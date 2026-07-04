package app.funput.funput.keyboard

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.KeyboardTouchHandler
import app.funput.funput.keyboard.layout.KeyboardGeometry
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.KeyboardLayouts
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.rendering.KeyboardCanvasRenderer
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/**
 * Low-allocation keyboard surface shared by the future IME and preview application.
 *
 * Layout, rendering, and touch tracking are delegated so this class only coordinates Android's
 * [View] lifecycle.
 */
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    var inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX
        set(value) {
            if (field == value) return
            interaction.reset()
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
            renderer.updateTheme(value, width, height)
            invalidate()
        }

    var suggestions: List<String> = emptyList()
        set(value) {
            val normalized = SuggestionNormalizer.normalize(value)
            if (field == normalized) return
            field = normalized
            invalidate()
        }

    var onKeyAction: ((KeyAction) -> Unit)? = null
    val shiftState: ShiftState get() = interaction.shiftState
    var language: KeyboardLanguage
        get() = interaction.language
        set(value) = interaction.setLanguage(value)

    private var keyboardLayout: KeyboardLayout = KeyboardLayouts.forInputMethod(inputMethod)
    private var resolvedKeyboard: ResolvedKeyboard? = null
    private val renderer = KeyboardCanvasRenderer(resources)
    private val interaction = KeyboardSurfaceInteraction(
        keyAt = { x, y -> resolvedKeyboard?.keyAt(x, y)?.spec?.id },
        keySpec = { id -> resolvedKeyboard?.keys?.firstOrNull { it.spec.id == id }?.spec },
        onAction = { action -> onKeyAction?.invoke(action) },
        onVisualStateChanged = ::postInvalidateOnAnimation,
        schedule = { task, delay -> postDelayed(task, delay) },
        cancel = ::removeCallbacks,
        requestParentIntercept = { disallow -> parent?.requestDisallowInterceptTouchEvent(disallow) },
        doubleTapTimeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong(),
        density = resources.displayMetrics.density,
    )

    init {
        renderer.updateTheme(keyboardTheme, width, height)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        setMeasuredDimension(
            resolveSize((KeyboardDimensions.DefaultWidthDp * density).roundToInt(), widthMeasureSpec),
            resolveSize((recommendedHeightDp(inputMethod) * density).roundToInt(), heightMeasureSpec),
        )
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resolveGeometry()
        renderer.updateTheme(keyboardTheme, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val keyboard = resolvedKeyboard ?: return
        renderer.draw(canvas, width, height, keyboard, suggestions, interaction, shiftState, language)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = when (interaction.onTouchEvent(event)) {
        KeyboardTouchHandler.Result.UNHANDLED -> false
        KeyboardTouchHandler.Result.HANDLED -> true
        KeyboardTouchHandler.Result.CLICK -> {
            performClick()
            true
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) interaction.clear()
    }

    override fun onDetachedFromWindow() {
        interaction.clear()
        super.onDetachedFromWindow()
    }

    private fun resolveGeometry() {
        if (width <= 0 || height <= 0) return
        resolvedKeyboard = KeyboardGeometry.resolve(
            layout = keyboardLayout,
            width = width.toFloat(),
            height = height.toFloat(),
            spec = KeyboardGeometrySpec.fromDensity(resources.displayMetrics.density),
        )
    }

    companion object {
        fun recommendedHeightDp(inputMethod: KeyboardInputMethod): Float =
            KeyboardDimensions.recommendedHeightDp(inputMethod)
    }
}
