package app.funput.funput.keyboard

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import app.funput.funput.keyboard.accessibility.KeyboardAccessibilityDelegate
import app.funput.funput.keyboard.accessibility.KeyboardAccessibilitySnapshot
import app.funput.funput.keyboard.interaction.interactionTargetAt
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.KeyboardTouchHandler
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.resolveGeometry
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.rendering.KeyboardCanvasRenderer
import kotlin.math.roundToInt

/** Low-allocation keyboard surface that coordinates Android's [View] lifecycle. */
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val layoutState = KeyboardSurfaceLayoutState(::updateKeyboardLayout)
    private val renderer = KeyboardCanvasRenderer(resources)
    private val presentation = KeyboardSurfacePresentationState(
        onThemeChanged = { renderer.updateTheme(it, width, height); invalidate() },
        onSizingChanged = { renderer.updateSizing(it); requestLayout(); resolveGeometry(); invalidate() },
        onSuggestionsChanged = ::invalidate,
        onEnterActionChanged = ::invalidate,
    )
    var inputMethod: KeyboardInputMethod by layoutState::inputMethod
    var layoutMode: KeyboardLayoutMode by layoutState::layoutMode
    var editorMode: KeyboardEditorMode by layoutState::editorMode
    var suggestionBarEnabled: Boolean by layoutState::suggestionsEnabled
    var systemInputMethodSwitcherVisible: Boolean by layoutState::systemInputMethodSwitcherVisible
    var keyboardTheme by presentation::keyboardTheme
    var sizingProfile: KeyboardSizingProfile by presentation::sizingProfile
    var suggestions: List<String> by presentation::suggestions
    var enterAction by presentation::enterAction
    val callbacks = KeyboardCallbacks()
    var shiftState: ShiftState
        get() = interaction.shiftState
        set(value) = interaction.setShiftState(value)
    var language: KeyboardLanguage
        get() = interaction.language
        set(value) = interaction.setLanguage(value)
    private var resolvedKeyboard: ResolvedKeyboard? = null
    private var accessibilitySnapshot: KeyboardAccessibilitySnapshot? = null // Shared by TalkBack callbacks.
    private val accessibility: KeyboardAccessibilityDelegate = KeyboardAccessibilityDelegate(
        host = this,
        snapshot = { accessibilitySnapshot },
        activate = ::performAccessibilityClick,
    )
    private val interaction: KeyboardSurfaceInteraction = KeyboardSurfaceInteraction(
        keyAt = { x, y -> resolvedKeyboard?.interactionTargetAt(x, y, suggestions.size) },
        keySpec = { id -> resolvedKeyboard?.keys?.firstOrNull { it.spec.id == id }?.spec },
        suggestionSelection = { id -> suggestions.selectionForTarget(id) },
        onAction = callbacks::dispatch,
        onSettingsRequested = callbacks::dispatchSettingsRequest,
        onEmojiRequested = callbacks::dispatchEmojiRequest,
        onSuggestionSelected = callbacks::dispatchSuggestion,
        onHapticFeedback = { type ->
            KeyboardHaptics.perform(this, type)
            KeyboardSounds.perform(this, type)
        },
        onVisualStateChanged = ::postInvalidateOnAnimation,
        onSemanticStateChanged = ::invalidateAccessibility,
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
        renderer.draw(canvas, width, height, keyboard, suggestions, interaction,
            shiftState, language, enterAction, editorMode.isPassword)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean = when (interaction.onTouchEvent(event)) {
        KeyboardTouchHandler.Result.UNHANDLED -> false
        KeyboardTouchHandler.Result.HANDLED -> true
        KeyboardTouchHandler.Result.CLICK -> performClick()
    }
    override fun dispatchHoverEvent(event: MotionEvent): Boolean = accessibility.dispatchHover(event) ||
        super.dispatchHoverEvent(event)
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
        resolvedKeyboard = layoutState.layout.resolveGeometry(
            width = width,
            height = height,
            density = resources.displayMetrics.density,
            profile = sizingProfile,
        )
        refreshAccessibilitySnapshot()
    }
    private fun updateKeyboardLayout() {
        interaction.reset()
        requestLayout()
        resolveGeometry()
        invalidate()
    }
    private fun performAccessibilityClick(keyId: String) =
        interaction.performAccessibilityClick(keyId, SystemClock.uptimeMillis())
    private fun invalidateAccessibility() = refreshAccessibilitySnapshot()
    private fun refreshAccessibilitySnapshot() {
        accessibilitySnapshot = resolvedKeyboard?.let { KeyboardAccessibilitySnapshot(it, shiftState) }
        accessibility.invalidateRoot()
    }
}
