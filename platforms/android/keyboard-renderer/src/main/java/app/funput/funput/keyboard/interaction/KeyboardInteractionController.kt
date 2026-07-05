package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection

/** Coordinates touch-driven behaviors without coupling them to the Android view lifecycle. */
internal class KeyboardInteractionController(
    private val keySpec: (keyId: String) -> KeySpec?,
    private val suggestionSelection: (targetId: String) -> SuggestionSelection?,
    onAction: (KeyAction) -> Unit,
    private val onSettingsRequested: () -> Unit,
    private val onEmojiRequested: () -> Unit,
    private val onSuggestionSelected: (SuggestionSelection) -> Unit,
    private val onHapticFeedback: (KeyboardHapticType) -> Unit,
    private val onVisualStateChanged: () -> Unit,
    schedule: (task: Runnable, delayMillis: Long) -> Unit,
    cancel: (task: Runnable) -> Unit,
    doubleTapTimeoutMillis: Long,
    density: Float,
) {
    private val actionDispatcher = KeyboardActionDispatcher(
        keySpec = keySpec,
        onAction = onAction,
        onShiftStateChanged = onVisualStateChanged,
        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
    )
    private val backspaceRepeat = BackspaceRepeatController(
        schedule = schedule,
        cancel = cancel,
        onRepeat = {
            actionDispatcher.repeatBackspace()
            onHapticFeedback(KeyboardHapticType.DELETE_REPEAT)
        },
    )
    private val swipeGestures = KeySwipeGestureTracker.fromDensity(density)

    val shiftState: ShiftState get() = actionDispatcher.shiftState
    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set

    fun onPointerStarted(pointerId: Int, keyId: String?, x: Float, y: Float) {
        val key = keyId?.let(keySpec)
        KeyHapticTypeMapper.forTarget(key, keyId?.let(suggestionSelection) != null)?.let(onHapticFeedback)
        swipeGestures.start(pointerId, key, x, y)
    }

    fun onPointerKeyChanged(pointerId: Int, keyId: String?) {
        val isBackspace = keyId != null && keySpec(keyId)?.role == KeyRole.BACKSPACE
        backspaceRepeat.update(pointerId, isBackspace)
    }

    fun onKeyReleased(pointerId: Int, keyId: String?, x: Float, y: Float, eventTimeMillis: Long) {
        val selection = keyId?.let(suggestionSelection)
        val key = keyId?.let(keySpec)
        val isBackspace = key?.role == KeyRole.BACKSPACE
        val swipeAction = swipeGestures.finish(pointerId, key, x, y)
        if (backspaceRepeat.finish(pointerId, isBackspace)) return

        when {
            selection != null -> onSuggestionSelected(selection)
            key?.role == KeyRole.SETTINGS -> onSettingsRequested()
            key?.role == KeyRole.EMOJI -> onEmojiRequested()
            swipeAction == KeySwipeAction.TOGGLE_LANGUAGE -> {
                setLanguage(language.toggled())
                actionDispatcher.toggleLanguage(language)
            }
            keyId != null -> actionDispatcher.dispatch(keyId, eventTimeMillis)
        }
    }

    fun setLanguage(value: KeyboardLanguage) {
        if (language == value) return
        language = value
        onVisualStateChanged()
    }

    fun setShiftState(value: ShiftState) = actionDispatcher.setShiftState(value)

    fun cancel() {
        backspaceRepeat.cancelAll()
        swipeGestures.cancel()
    }

    fun reset() {
        cancel()
        actionDispatcher.reset()
    }
}
