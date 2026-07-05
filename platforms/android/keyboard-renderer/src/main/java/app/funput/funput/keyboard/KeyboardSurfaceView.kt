package app.funput.funput.keyboard

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.funput.funput.keyboard.interaction.interactionTargetAt
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.KeyboardTouchHandler
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.resolveGeometry
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.rendering.KeyboardCanvasRenderer
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Low-allocation keyboard surface that coordinates Android's [View] lifecycle. */
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    var inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX
        set(value) {
            if (field == value) return
            field = value
            updateKeyboardLayout()
        }
    var layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.LETTERS
        set(value) {
            if (field == value) return
            field = value
            updateKeyboardLayout()
        }
    var editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT
        set(value) {
            if (field == value) return
            field = value
            updateKeyboardLayout()
        }
    var suggestionBarEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            updateKeyboardLayout()
        }
    var keyboardTheme: KeyboardTheme = KeyboardTheme.Aurora
        set(value) {
            if (field == value) return
            field = value
            renderer.updateTheme(value, width, height)
            invalidate()
        }
    var sizingProfile: KeyboardSizingProfile = KeyboardSizingProfile.Default
        set(value) {
            if (field == value) return
            field = value
            renderer.updateSizing(value)
            requestLayout()
            resolveGeometry()
            invalidate()
        }
    var suggestions: List<String> = emptyList()
        set(value) {
            val normalized = SuggestionNormalizer.normalize(value)
            if (field == normalized) return
            field = normalized
            invalidate()
        }
    var enterAction: KeyboardEnterAction = KeyboardEnterAction.Standard.NEW_LINE
        set(value) { field = value; invalidate() }
    val callbacks = KeyboardCallbacks()
    var shiftState: ShiftState
        get() = interaction.shiftState
        set(value) = interaction.setShiftState(value)
    var language: KeyboardLanguage
        get() = interaction.language
        set(value) = interaction.setLanguage(value)
    private var keyboardLayout: KeyboardLayout = KeyboardLayoutResolver.resolve(inputMethod, layoutMode, editorMode, suggestionBarEnabled)
    private var resolvedKeyboard: ResolvedKeyboard? = null
    private val renderer = KeyboardCanvasRenderer(resources)
    private val interaction = KeyboardSurfaceInteraction(
        keyAt = { x, y -> resolvedKeyboard?.interactionTargetAt(x, y, suggestions.size) },
        keySpec = { id -> resolvedKeyboard?.keys?.firstOrNull { it.spec.id == id }?.spec },
        suggestionSelection = { id -> suggestions.selectionForTarget(id) },
        onAction = callbacks::dispatch,
        onEmojiRequested = callbacks::dispatchEmojiRequest,
        onSuggestionSelected = callbacks::dispatchSuggestion,
        onHapticFeedback = { type ->
            KeyboardHaptics.perform(this, type)
            KeyboardSounds.perform(this, type)
        },
        onVisualStateChanged = ::postInvalidateOnAnimation,
        schedule = { task, delay -> postDelayed(task, delay) },
        cancel = ::removeCallbacks,
        requestParentIntercept = { disallow -> parent?.requestDisallowInterceptTouchEvent(disallow) },
        doubleTapTimeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong(),
        density = resources.displayMetrics.density,
    )
    init {
        renderer.updateTheme(keyboardTheme, width, height)
        renderer.updateSizing(sizingProfile)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val width = resolveSize((KeyboardDimensions.DefaultWidthDp * density).roundToInt(), widthMeasureSpec)
        val heightDp = KeyboardDimensions.recommendedHeightDp(inputMethod, editorMode, sizingProfile)
        val height = resolveSize((heightDp * density).roundToInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resolveGeometry()
        renderer.updateTheme(keyboardTheme, width, height)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val keyboard = resolvedKeyboard ?: return
        renderer.draw(canvas, width, height, keyboard, suggestions, interaction, shiftState, language, enterAction)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean = when (interaction.onTouchEvent(event)) {
        KeyboardTouchHandler.Result.UNHANDLED -> false
        KeyboardTouchHandler.Result.HANDLED -> true
        KeyboardTouchHandler.Result.CLICK -> performClick()
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
        resolvedKeyboard = keyboardLayout.resolveGeometry(
            width = width,
            height = height,
            density = resources.displayMetrics.density,
            profile = sizingProfile,
        )
    }
    private fun updateKeyboardLayout() {
        interaction.reset()
        keyboardLayout = KeyboardLayoutResolver.resolve(inputMethod, layoutMode, editorMode, suggestionBarEnabled)
        requestLayout()
        resolveGeometry()
        invalidate()
    }
}
