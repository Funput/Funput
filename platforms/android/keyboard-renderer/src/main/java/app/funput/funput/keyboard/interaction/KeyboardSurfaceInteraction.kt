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
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment
import app.funput.funput.keyboard.suggestion.SuggestionChip
import app.funput.funput.keyboard.suggestion.actions.SuggestionAction
import app.funput.funput.keyboard.suggestion.actions.SuggestionActions
import app.funput.funput.keyboard.touch.KeyTouchRecorder

/** Wires Android touch routing to renderer-independent keyboard interaction state. */
internal class KeyboardSurfaceInteraction(
    keyAt: (x: Float, y: Float) -> String?,
    keyboard: () -> ResolvedKeyboard?,
    keySpec: (keyId: String) -> KeySpec?,
    suggestions: () -> List<SuggestionChip>,
    onAction: (KeyAction) -> Unit,
    utilities: KeyboardUtilityDispatcher,
    onSuggestionSelected: (SuggestionSelection) -> Unit,
    suggestionEditable: (SuggestionSelection) -> Boolean,
    onSuggestionAction: (SuggestionSelection, SuggestionAction) -> Unit,
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
    private val suggestionActions = SuggestionActions(
        suggestions, schedule, cancel, touchSlop, suggestionEditable, onSuggestionAction,
        onFeedback = { onHapticFeedback(KeyboardHapticType.CONTROL) },
        capture = { pointerId -> touchHandler.capture(pointerId) },
        onChanged = { onVisualStateChanged(); onSemanticStateChanged() },
    )
    private val controller = KeyboardInteractionController(
        keySpec = keySpec,
        suggestionSelection = { id -> suggestions().selectionForTarget(id) },
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
            keyAt = { x, y -> suggestionActions.targetAt(x, y, keyboard()) ?: keyAt(x, y) },
            onPressedStateChanged = onVisualStateChanged,
            onPointerStarted = { id, key, x, y ->
                touches.down(id, x, y)
                suggestionActions.started(id, key, x, y)
                controller.onPointerStarted(id, key, x, y)
            },
            onPointerKeyChanged = controller::onPointerKeyChanged,
            onPointerMoved = { id, key, x, y -> suggestionActions.moved(id, x, y); controller.onPointerMoved(id, key, x, y) },
            onKeyReleased = { id, key, x, y, time ->
                // A held word or a slot of the strip is spent here; the controller still
                // hears the release, keyless, so its own per-pointer state is let go.
                val target = suggestionActions.released(id, key)
                touches.releasing(id, target) { controller.onKeyReleased(id, target, x, y, time) }
            },
            isPointerCaptured = { id -> controller.isPointerCaptured(id) || suggestionActions.isCaptured(id) },
            onCancelled = { suggestionActions.cancel(); controller.cancel() },
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

    /** The actions a long press opened, drawn in place of the candidates; empty while closed. */
    fun suggestionActionSegments(keyboard: ResolvedKeyboard): List<SuggestionBandSegment> =
        suggestionActions.segments(keyboard)

    fun isSuggestionEditable(selection: SuggestionSelection): Boolean = suggestionActions.isEditable(selection)

    fun setShiftState(value: ShiftState) = controller.setShiftState(value)

    fun onTouchEvent(event: MotionEvent): KeyboardTouchHandler.Result = touchHandler.onTouchEvent(event)

    fun performAccessibilitySuggestionClick(targetId: String) =
        controller.emitSuggestion(targetId)

    fun performAccessibilityClick(keyId: String, eventTimeMillis: Long) =
        controller.emitClick(keyId, eventTimeMillis)

    fun performAccessibilityAlternate(keyId: String, index: Int) =
        controller.emitAlternate(keyId, index)

    fun performAccessibilityAction(action: KeyAction) = controller.emitAction(action)

    fun performAccessibilitySuggestionAction(targetId: String, action: SuggestionAction) =
        suggestionActions.perform(targetId, action)

    fun clear() {
        touchHandler.clear()
        touches.clear()
        suggestionActions.close()
    }

    fun reset() {
        clear()
        controller.reset()
    }

    override fun isPressed(keyId: String): Boolean = touchHandler.isPressed(keyId)
}
