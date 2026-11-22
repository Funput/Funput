package app.funput.funput.keyboard.interaction
import app.funput.funput.keyboard.utility.KeyboardUtilityAction
import app.funput.funput.keyboard.utility.KeyboardUtilityDispatcher
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.interaction.clipboard.ClipboardKeyHold
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.popover.interaction.AlternateSelectionController
import app.funput.funput.keyboard.popover.interaction.AlternateSelectionPreview
import app.funput.funput.keyboard.interaction.backspace.BackspaceRepeatController
import app.funput.funput.keyboard.interaction.backspace.BackspaceRepeatStage
import app.funput.funput.keyboard.interaction.gestures.SmartGestureCoordinator

internal class KeyboardInteractionController(
    private val keySpec: (keyId: String) -> KeySpec?,
    private val suggestionSelection: (targetId: String) -> SuggestionSelection?,
    private val onAction: (KeyAction) -> Unit,
    private val utilities: KeyboardUtilityDispatcher = KeyboardUtilityDispatcher {},
    onPointersCancelled: () -> Unit = {},
    private val onSuggestionSelected: (SuggestionSelection) -> Unit,
    private val onHapticFeedback: (KeyboardHapticType) -> Unit,
    private val onVisualStateChanged: () -> Unit,
    private val onSemanticStateChanged: () -> Unit,
    schedule: (task: Runnable, delayMillis: Long) -> Unit,
    cancel: (task: Runnable) -> Unit,
    keyBounds: (keyId: String) -> KeyBounds?,
    surfaceBounds: () -> KeyBounds,
    touchSlop: Float,
    onPointerCaptured: (Int) -> Unit,
    doubleTapTimeoutMillis: Long,
    density: Float,
    hasPasteOffer: () -> Boolean = { false },
) {
    private val actionDispatcher = KeyboardActionDispatcher(
        keySpec = keySpec,
        onAction = onAction,
        onShiftStateChanged = { onVisualStateChanged(); onSemanticStateChanged() },
        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
    )
    private val utilityActions = KeyboardUtilityActionRouter(onSuggestionSelected, utilities, hasPasteOffer)
    private val alternateActions = AlternateActionRouter(
        actionDispatcher, { cancel(); onPointersCancelled() },
        { utilities.dispatch(KeyboardUtilityAction.PLACEMENT) },
        { utilities.dispatch(KeyboardUtilityAction.SETTINGS) },
    )
    private val backspaceRepeat = BackspaceRepeatController(
        schedule = schedule, cancel = cancel,
        onRepeat = { stage ->
            actionDispatcher.repeatBackspace(stage)
            onHapticFeedback(if (stage == BackspaceRepeatStage.Word) KeyboardHapticType.DELETE else KeyboardHapticType.DELETE_REPEAT)
        },
    )
    private val swipeGestures = KeySwipeGestureTracker.fromDensity(density)
    private val gestures = SmartGestureCoordinator(
        density, schedule, cancel, onAction, onHapticFeedback, onPointerCaptured,
        { backspaceRepeat.hasRepeated }, { id -> backspaceRepeat.update(id, false) },
    )
    private val clipboardHold = ClipboardKeyHold(schedule, cancel, touchSlop) { pointerId ->
        onPointerCaptured(pointerId)
        utilities.dispatch(KeyboardUtilityAction.CLIPBOARD_PANEL)
        onHapticFeedback(KeyboardHapticType.CONTROL)
    }
    var areSmartGesturesEnabled by gestures::enabled
    var breaksLineOnEnterHold by gestures::breaksLineOnEnterHold
    private val alternateSelection = AlternateSelectionController(
        keyBounds = keyBounds, surfaceBounds = surfaceBounds, schedule = schedule, cancel = cancel,
        touchSlop = touchSlop, density = density, onCaptured = onPointerCaptured,
        onFeedback = { onHapticFeedback(KeyboardHapticType.CONTROL) },
        onChanged = onVisualStateChanged, onSelected = alternateActions::dispatch,
    )
    val shiftState: ShiftState get() = actionDispatcher.shiftState
    val alternatePreview: AlternateSelectionPreview? get() = alternateSelection.preview
    fun isPointerCaptured(pointerId: Int) =
        alternateSelection.isCaptured(pointerId) || gestures.isCaptured(pointerId) ||
            clipboardHold.isClaimed(pointerId)
    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set
    fun onPointerStarted(pointerId: Int, keyId: String?, x: Float, y: Float) {
        val key = keyId?.let(keySpec)
        KeyHapticTypeMapper.forTarget(key, keyId?.let(suggestionSelection) != null, keyId)?.let(onHapticFeedback)
        swipeGestures.start(pointerId, key, x, y)
        alternateSelection.start(pointerId, key, x, y)
        gestures.onStarted(pointerId, key, x, y)
        clipboardHold.start(pointerId, key, x, y)
    }
    fun onPointerKeyChanged(pointerId: Int, keyId: String?) {
        backspaceRepeat.update(pointerId, keyId != null && keySpec(keyId)?.role == KeyRole.BACKSPACE)
    }
    fun onPointerMoved(pointerId: Int, keyId: String?, x: Float, y: Float) {
        clipboardHold.moved(pointerId, x, y)
        if (!gestures.onMoved(pointerId, x, y)) alternateSelection.move(pointerId, keyId, x, y)
    }
    fun onKeyReleased(pointerId: Int, keyId: String?, x: Float, y: Float, eventTimeMillis: Long) {
        if (alternateSelection.finish(pointerId, x, y)) {
            forgetPointer(pointerId); return
        }
        if (gestures.onReleased(pointerId)) {
            swipeGestures.forget(pointerId); clipboardHold.finish(pointerId); return
        }
        if (clipboardHold.finish(pointerId)) {
            swipeGestures.forget(pointerId); return
        }
        val key = keyId?.let(keySpec)
        val swipeAction = swipeGestures.finish(pointerId, x, y)
        if (backspaceRepeat.finish(pointerId, key?.role == KeyRole.BACKSPACE)) return
        if (swipeAction == KeySwipeAction.TOGGLE_LANGUAGE) {
            setLanguage(language.toggled()); actionDispatcher.toggleLanguage(language)
        } else {
            utilityActions.dispatch(keyId, key, keyId?.let(suggestionSelection)) {
                actionDispatcher.dispatch(it, eventTimeMillis)
            }
        }
    }
    fun emitClick(keyId: String, eventTimeMillis: Long) {
        KeyHapticTypeMapper.forTarget(keySpec(keyId), false, keyId)?.let(onHapticFeedback)
        utilityActions.dispatch(keyId, keySpec(keyId), null) {
            actionDispatcher.dispatch(it, eventTimeMillis)
        }
    }
    fun emitClipboardPanel() {
        utilities.dispatch(KeyboardUtilityAction.CLIPBOARD_PANEL); onHapticFeedback(KeyboardHapticType.CONTROL)
    }
    fun emitAlternate(keyId: String, index: Int) {
        val key = keySpec(keyId) ?: return
        key.alternates.getOrNull(index)?.let { alternateActions.dispatch(key, it) }
    }
    fun emitSuggestion(targetId: String) = suggestionSelection(targetId)?.let {
        onHapticFeedback(KeyboardHapticType.CONTROL); onSuggestionSelected(it)
    }
    fun emitAction(action: KeyAction) = onAction(action)
    fun setLanguage(value: KeyboardLanguage) {
        if (language == value) return
        language = value.also { onVisualStateChanged(); onSemanticStateChanged() }
    }
    fun setShiftState(value: ShiftState) = actionDispatcher.setShiftState(value)
    fun cancel() {
        alternateSelection.cancelAll(); backspaceRepeat.cancelAll()
        swipeGestures.cancel(); gestures.cancelAll(); clipboardHold.cancelAll()
    }
    fun reset() { cancel(); actionDispatcher.reset() }
    private fun forgetPointer(id: Int) {
        swipeGestures.forget(id); gestures.forget(id); clipboardHold.finish(id)
    }
}
