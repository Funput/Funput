package app.funput.funput.keyboard.interaction

import android.view.MotionEvent
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.popover.interaction.AlternateSelectionPreview

/** Wires Android touch routing to renderer-independent keyboard interaction state. */
internal class KeyboardSurfaceInteraction(
    keyAt: (x: Float, y: Float) -> String?,
    keySpec: (keyId: String) -> KeySpec?,
    suggestionSelection: (targetId: String) -> SuggestionSelection?,
    onAction: (KeyAction) -> Unit,
    onEmojiRequested: () -> Unit,
    onClipboardPanelRequested: () -> Unit,
    onClipboardPasteRequested: () -> Unit,
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
    private val controller = KeyboardInteractionController(
        keySpec = keySpec,
        suggestionSelection = suggestionSelection,
        onAction = onAction,
        onEmojiRequested = onEmojiRequested,
        onClipboardPanelRequested = onClipboardPanelRequested,
        onClipboardRequested = onClipboardPasteRequested,
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
            onPointerStarted = controller::onPointerStarted,
            onPointerKeyChanged = controller::onPointerKeyChanged,
            onPointerMoved = controller::onPointerMoved,
            onKeyReleased = controller::onKeyReleased,
            isPointerCaptured = controller::isAlternateCaptured,
            onCancelled = controller::cancel,
            requestParentIntercept = requestParentIntercept,
        )
    }

    val shiftState: ShiftState get() = controller.shiftState
    val language: KeyboardLanguage get() = controller.language
    val alternatePreview: AlternateSelectionPreview? get() = controller.alternatePreview

    fun setLanguage(value: KeyboardLanguage) = controller.setLanguage(value)

    fun setShiftState(value: ShiftState) = controller.setShiftState(value)

    fun onTouchEvent(event: MotionEvent): KeyboardTouchHandler.Result = touchHandler.onTouchEvent(event)

    fun performAccessibilitySuggestionClick(targetId: String) =
        controller.onAccessibilitySuggestion(targetId)

    fun performAccessibilityClick(keyId: String, eventTimeMillis: Long) =
        controller.onAccessibilityClick(keyId, eventTimeMillis)

    fun performAccessibilityAlternate(keyId: String, index: Int) =
        controller.onAccessibilityAlternate(keyId, index)

    fun clear() = touchHandler.clear()

    fun reset() {
        touchHandler.clear()
        controller.reset()
    }

    override fun isPressed(keyId: String): Boolean = touchHandler.isPressed(keyId)
}
