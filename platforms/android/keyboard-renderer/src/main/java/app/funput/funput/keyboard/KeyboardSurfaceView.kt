package app.funput.funput.keyboard
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.interaction.interactionTargetAt
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.resolveGeometry
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.surface.KeyboardSurfaceEventDispatcher
import app.funput.funput.keyboard.surface.KeyboardSurfaceAccessibilityBinding
import app.funput.funput.keyboard.surface.KeyboardSurfaceLayoutState
import app.funput.funput.keyboard.surface.KeyboardSurfaceOverlayPad
import app.funput.funput.keyboard.surface.KeyboardSurfaceRenderController
import app.funput.funput.keyboard.surface.createKeyboardSurfaceInteraction
import kotlin.math.roundToInt
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val overlay = KeyboardSurfaceOverlayPad(::requestLayout) { onOverlayPadChanged?.invoke(it) }
    val overlayPadTop: Int get() = overlay.pixels
    var onOverlayPadChanged: ((Int) -> Unit)? = null
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
    var layoutOverride: app.funput.funput.keyboard.model.KeyboardLayout? by layoutState::layoutOverride
    var suggestionBarEnabled: Boolean by layoutState::suggestionsEnabled
    var systemInputMethodSwitcherVisible: Boolean by layoutState::systemInputMethodSwitcherVisible
    var clipboardKeyVisible: Boolean = false; set(value) { if (field != value) { field = value; resolveGeometry() } }
    var showsNumberRow: Boolean by layoutState::showsNumberRow
    var keyboardTheme by render::keyboardTheme
    var keyboardThemeBackgroundImage by render::keyboardThemeBackgroundImage
    var sizingProfile: KeyboardSizingProfile by render::sizingProfile
    var suggestions: List<String>
        get() = render.suggestions
        set(value) = suggestionState.update(value)
    var clipboardHint: KeyboardClipboardHint?
        get() = render.clipboardHint
        set(value) { render.clipboardHint = value; accessibility.refresh() }
    var enterAction by render::enterAction
    val callbacks = KeyboardCallbacks()
    var shiftState: ShiftState
        get() = interaction.shiftState
        set(value) = interaction.setShiftState(value)
    var language: KeyboardLanguage get() = interaction.language; set(value) { interaction.language = value }
    var areSmartGesturesEnabled: Boolean get() = interaction.areSmartGesturesEnabled; set(value) { interaction.areSmartGesturesEnabled = value }
    private var resolvedKeyboard: ResolvedKeyboard? = null
    private val accessibility = KeyboardSurfaceAccessibilityBinding(
        host = this,
        interaction = { interaction },
        keyboard = { resolvedKeyboard },
        shiftState = { shiftState },
        suggestions = { render.suggestions },
        clipboardHint = { render.clipboardHint },
    )
    private val suggestionState = KeyboardSurfaceSuggestionState(
        density = { resources.displayMetrics.density },
        keyboard = { resolvedKeyboard },
        apply = { values -> render.suggestions = values; accessibility.refresh() },
        onUtilityKeysVisibilityChanged = { resolveGeometry() },
    )
    private val interaction: app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction = createKeyboardSurfaceInteraction(
        host = this,
        callbacks = callbacks,
        keyAt = { x, y -> resolvedKeyboard?.interactionTargetAt(
            x, y, suggestions.size, clipboardHint != null && suggestions.isEmpty(),
        ) },
        keySpec = { id -> resolvedKeyboard?.keys?.firstOrNull { it.spec.id == id }?.spec },
        suggestionSelection = { id -> suggestions.selectionForTarget(id) },
        onHapticFeedback = { type ->
            KeyboardHaptics.perform(this, type)
            KeyboardSounds.perform(this, type)
        },
        onVisualStateChanged = {
            overlay.sync(interaction.alternatePreview?.layout?.overflowAbove ?: 0f)
            postInvalidateOnAnimation()
        },
        onSemanticStateChanged = accessibility::refresh,
        keyBounds = { id -> resolvedKeyboard?.keys?.firstOrNull { it.spec.id == id }?.bounds },
        surfaceBounds = { KeyBounds(0f, 0f, width.toFloat(), overlay.keyboardHeight(height).toFloat()) },
    )
    private val events = KeyboardSurfaceEventDispatcher(
        host = this,
        interaction = interaction,
        dispatchAccessibilityHover = accessibility::dispatchHover,
    )
    var interactionEnabled: Boolean
        get() = events.enabled
        set(value) = events.setEnabled(value)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val width = resolveSize((KeyboardDimensions.DefaultWidthDp * density).roundToInt(), widthMeasureSpec)
        val heightDp = KeyboardDimensions.recommendedHeightDp(
            inputMethod, editorMode, sizingProfile, width / density, showsNumberRow,
        )
        val height = resolveSize((heightDp * density).roundToInt() + overlay.pixels, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resolveGeometry()
        render.updateSize(width, overlay.keyboardHeight(height))
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        overlay.drawTranslated(canvas) {
            resolvedKeyboard?.let { render.draw(canvas, it, interaction, shiftState, language, editorMode) }
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean =
        overlay.withKeyboardCoordinates(event) { events.dispatchTouch(event, ::performClick) }
    override fun dispatchHoverEvent(event: MotionEvent): Boolean = overlay.withKeyboardCoordinates(event) {
        events.dispatchHover(event) { super.dispatchHoverEvent(event) }
    }
    override fun performClick(): Boolean {
        if (!events.enabled) return false
        super.performClick(); return true
    }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus); if (!hasWindowFocus) interaction.clear()
    }
    override fun onDetachedFromWindow() { interaction.clear(); render.clear(); super.onDetachedFromWindow() }
    private fun resolveGeometry() {
        resolvedKeyboard = layoutState.layout.resolveGeometry(
            width = width, height = overlay.keyboardHeight(height),
            density = resources.displayMetrics.density, profile = sizingProfile,
            showClipboard = clipboardKeyVisible && suggestionState.utilityKeysVisible,
        )
        suggestionState.geometryChanged(); accessibility.refresh()
    }
    private fun updateKeyboardLayout() { interaction.reset(); requestLayout(); resolveGeometry(); invalidate() }
}
