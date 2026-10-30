package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.utility.KeyboardUtilityDispatcher
import android.view.MotionEvent
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.popover.interaction.AlternateSelectionPreview
import app.funput.funput.keyboard.touch.KeyTouchRecorder

/** Wires Android touch routing to renderer-independent keyboard interaction state. */
internal class KeyboardSurfaceInteraction(
    keyAt: (x: Float, y: Float) -> String?,
    keyboard: () -> ResolvedKeyboard?,
    keySpec: (keyId: String) -> KeySpec?,
    suggestionSelection: (targetId: String) -> SuggestionSelection?,
    onAction: (KeyAction) -> Unit,
    utilities: KeyboardUtilityDispatcher,
    onSuggestionSelected: (SuggestionSelection) -> Unit,
    onHapticFeedback: (KeyboardHapticType) -> Unit,
    onVisualStateChanged: () -> Unit,
    onSemanticStateChanged: () -> Unit,
    schedule: (task: Runnable, delayMillis: Long) -> Unit,
    cancel: (task: Runnable) -> Unit,
    requestParentIntercept: (disallow: Boolean) -> Unit,
    doubleTapTimeoutMillis: Long,
    density: Float,
    touchSlop: Float,
    keyBounds: (keyId: String) -> KeyBounds?,
    surfaceBounds: () -> KeyBounds,
) : PressedKeyState {
    private lateinit var touchHandler: KeyboardTouchHandler
    private val touches = KeyTouchRecorder(keyboard)
    private val controller = KeyboardInteractionController(
        keySpec = keySpec,
        suggestionSelection = suggestionSelection,
        onAction = { onAction(touches.attach(it)) },
        utilities = utilities,
        onPointersCancelled = { touchHandler.cancelGesture() },
        onSuggestionSelected = onSuggestionSelected,
        onHapticFeedback = onHapticFeedback,
        onVisualStateChanged = onVisualStateChanged,
        onSemanticStateChanged = onSemanticStateChanged,
        schedule = schedule,
        cancel = cancel,
        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
        density = density,
        touchSlop = touchSlop,
        keyBounds = keyBounds,
        surfaceBounds = surfaceBounds,
        onPointerCaptured = { pointerId -> touchHandler.capture(pointerId) },
    )
    init {
        touchHandler = KeyboardTouchHandler(
            keyAt = keyAt,
            onPressedStateChanged = onVisualStateChanged,
            onPointerStarted = { id, key, x, y -> touches.down(id, x, y); controller.onPointerStarted(id, key, x, y) },
            onPointerKeyChanged = controller::onPointerKeyChanged,
            onPointerMoved = controller::onPointerMoved,
            onKeyReleased = { id, key, x, y, time ->
                touches.releasing(id, key) { controller.onKeyReleased(id, key, x, y, time) }
            },
            isPointerCaptured = controller::isPointerCaptured,
            onCancelled = controller::cancel,
            requestParentIntercept = requestParentIntercept,
        )
    }

    val shiftState: ShiftState get() = controller.shiftState
    var language: KeyboardLanguage
        get() = controller.language
        set(value) = controller.setLanguage(value)
    var areSmartGesturesEnabled by controller::areSmartGesturesEnabled
    var breaksLineOnEnterHold by controller::breaksLineOnEnterHold
    val alternatePreview: AlternateSelectionPreview? get() = controller.alternatePreview

    fun setShiftState(value: ShiftState) = controller.setShiftState(value)

    fun onTouchEvent(event: MotionEvent): KeyboardTouchHandler.Result = touchHandler.onTouchEvent(event)

    fun performAccessibilitySuggestionClick(targetId: String) =
        controller.emitSuggestion(targetId)

    fun performAccessibilityClick(keyId: String, eventTimeMillis: Long) =
        controller.emitClick(keyId, eventTimeMillis)

    fun performAccessibilityAlternate(keyId: String, index: Int) =
        controller.emitAlternate(keyId, index)

    fun performAccessibilityAction(action: KeyAction) = controller.emitAction(action)

    fun clear() {
        touchHandler.clear()
        touches.clear()
    }

    fun reset() {
        clear()
        controller.reset()
    }

    override fun isPressed(keyId: String): Boolean = touchHandler.isPressed(keyId)
}
