package app.funput.funput.keyboard
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.accessibility.KeyboardSurfaceAccessibilityController
import app.funput.funput.keyboard.interaction.interactionTargetAt
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.resolveGeometry
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.surface.KeyboardSurfaceEventDispatcher
import app.funput.funput.keyboard.surface.KeyboardSurfaceLayoutState
import app.funput.funput.keyboard.surface.KeyboardSurfaceRenderController
import app.funput.funput.keyboard.surface.createKeyboardSurfaceInteraction
import kotlin.math.roundToInt
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val layoutState = KeyboardSurfaceLayoutState(::updateKeyboardLayout)
    private val render = KeyboardSurfaceRenderController(
        context = context,
        resources = resources,
        invalidate = ::invalidate,
        requestLayout = ::requestLayout,
        resolveGeometry = ::resolveGeometry,
    )
    var inputMethod: KeyboardInputMethod by layoutState::inputMethod
    var layoutMode: KeyboardLayoutMode by layoutState::layoutMode
    var editorMode: KeyboardEditorMode by layoutState::editorMode
    var suggestionBarEnabled: Boolean by layoutState::suggestionsEnabled
    var systemInputMethodSwitcherVisible: Boolean by layoutState::systemInputMethodSwitcherVisible
    var keyboardTheme by render::keyboardTheme
    var keyboardThemeBackgroundImage by render::keyboardThemeBackgroundImage
    var sizingProfile: KeyboardSizingProfile by render::sizingProfile
    var suggestions: List<String>
        get() = render.suggestions
        set(value) = suggestionState.update(value)
    var enterAction by render::enterAction
    val callbacks = KeyboardCallbacks()
    var shiftState: ShiftState
        get() = interaction.shiftState
        set(value) = interaction.setShiftState(value)
    var language: KeyboardLanguage
        get() = interaction.language
        set(value) = interaction.setLanguage(value)
    private var resolvedKeyboard: ResolvedKeyboard? = null
    private val accessibility = KeyboardSurfaceAccessibilityController(
        host = this,
        activate = ::performAccessibilityClick,
    )
    private val suggestionState = KeyboardSurfaceSuggestionState(
        density = { resources.displayMetrics.density },
        keyboard = { resolvedKeyboard },
        apply = { values -> render.suggestions = values; refreshAccessibilitySnapshot() },
    )
    private val interaction: KeyboardSurfaceInteraction = createKeyboardSurfaceInteraction(
        host = this,
        callbacks = callbacks,
        keyAt = { x, y -> resolvedKeyboard?.interactionTargetAt(x, y, suggestions.size) },
        keySpec = { id -> resolvedKeyboard?.keys?.firstOrNull { it.spec.id == id }?.spec },
        suggestionSelection = { id -> suggestions.selectionForTarget(id) },
        onHapticFeedback = { type ->
            KeyboardHaptics.perform(this, type)
            KeyboardSounds.perform(this, type)
        },
        onVisualStateChanged = ::postInvalidateOnAnimation,
        onSemanticStateChanged = ::invalidateAccessibility,
    )
    private val events = KeyboardSurfaceEventDispatcher(
        host = this,
        interaction = interaction,
        dispatchAccessibilityHover = accessibility::dispatchHover,
    )
    var interactionEnabled: Boolean
        get() = events.enabled
        set(value) {
            events.setEnabled(value)
        }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val width = resolveSize((KeyboardDimensions.DefaultWidthDp * density).roundToInt(), widthMeasureSpec)
        val heightDp = KeyboardDimensions.recommendedHeightDp(inputMethod, editorMode, sizingProfile, width / density)
        val height = resolveSize((heightDp * density).roundToInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resolveGeometry()
        render.updateSize(width, height)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val keyboard = resolvedKeyboard ?: return
        render.draw(canvas, keyboard, interaction, shiftState, language, editorMode)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean = events.dispatchTouch(event, ::performClick)
    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        events.dispatchHover(event) { super.dispatchHoverEvent(event) }
    override fun performClick(): Boolean {
        if (!events.enabled) return false
        super.performClick()
        return true
    }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) interaction.clear()
    }
    override fun onDetachedFromWindow() {
        interaction.clear()
        render.clear()
        super.onDetachedFromWindow()
    }
    private fun resolveGeometry() {
        resolvedKeyboard = layoutState.layout.resolveGeometry(
            width = width, height = height,
            density = resources.displayMetrics.density,
            profile = sizingProfile,
        )
        suggestionState.geometryChanged()
        refreshAccessibilitySnapshot()
    }
    private fun updateKeyboardLayout() {
        interaction.reset()
        requestLayout()
        resolveGeometry()
        invalidate()
    }
    private fun performAccessibilityClick(keyId: String) {
        val selection = render.suggestions.selectionForTarget(keyId)
        if (selection != null) interaction.performAccessibilitySuggestionClick(keyId) else {
            interaction.performAccessibilityClick(keyId, SystemClock.uptimeMillis())
        }
    }
    private fun invalidateAccessibility() = refreshAccessibilitySnapshot()
    private fun refreshAccessibilitySnapshot() {
        accessibility.refresh(resolvedKeyboard, shiftState, render.suggestions)
    }
}
