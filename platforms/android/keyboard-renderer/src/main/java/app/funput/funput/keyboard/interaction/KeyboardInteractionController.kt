package app.funput.funput.keyboard.interaction
import app.funput.funput.keyboard.KeyboardHapticType
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
internal class KeyboardInteractionController(
    private val keySpec: (keyId: String) -> KeySpec?,
    private val suggestionSelection: (targetId: String) -> SuggestionSelection?,
    onAction: (KeyAction) -> Unit,
    private val onSettingsRequested: () -> Unit,
    private val onEmojiRequested: () -> Unit,
    private val onClipboardPanelRequested: () -> Unit = {},
    private val onClipboardRequested: () -> Unit = {},
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
) {
    private val actionDispatcher = KeyboardActionDispatcher(
        keySpec = keySpec,
        onAction = onAction,
        onShiftStateChanged = {
            onVisualStateChanged()
            onSemanticStateChanged()
        },
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
    private val alternateSelection = AlternateSelectionController(
        keyBounds = keyBounds,
        surfaceBounds = surfaceBounds,
        schedule = schedule,
        cancel = cancel,
        touchSlop = touchSlop,
        density = density,
        onCaptured = onPointerCaptured,
        onFeedback = { onHapticFeedback(KeyboardHapticType.CONTROL) },
        onChanged = onVisualStateChanged,
        onSelected = actionDispatcher::dispatchAlternate,
    )
    val shiftState: ShiftState get() = actionDispatcher.shiftState
    val alternatePreview: AlternateSelectionPreview? get() = alternateSelection.preview
    fun isAlternateCaptured(pointerId: Int): Boolean = alternateSelection.isCaptured(pointerId)
    var language: KeyboardLanguage = KeyboardLanguage.VIETNAMESE
        private set
    fun onPointerStarted(pointerId: Int, keyId: String?, x: Float, y: Float) {
        val key = keyId?.let(keySpec)
        val isToolbarContent = keyId?.let(suggestionSelection) != null || keyId == ClipboardTargetId
        KeyHapticTypeMapper.forTarget(key, isToolbarContent)?.let(onHapticFeedback)
        swipeGestures.start(pointerId, key, x, y)
        alternateSelection.start(pointerId, key, x, y)
    }
    fun onPointerKeyChanged(pointerId: Int, keyId: String?) {
        val isBackspace = keyId != null && keySpec(keyId)?.role == KeyRole.BACKSPACE
        backspaceRepeat.update(pointerId, isBackspace)
    }
    fun onPointerMoved(pointerId: Int, keyId: String?, x: Float, y: Float) =
        alternateSelection.move(pointerId, keyId, x, y)
    fun onKeyReleased(pointerId: Int, keyId: String?, x: Float, y: Float, eventTimeMillis: Long) {
        if (alternateSelection.finish(pointerId, x, y)) {
            swipeGestures.finish(pointerId, null, x, y)
            return
        }
        val selection = keyId?.let(suggestionSelection)
        val key = keyId?.let(keySpec)
        val isBackspace = key?.role == KeyRole.BACKSPACE
        val swipeAction = swipeGestures.finish(pointerId, key, x, y)
        if (backspaceRepeat.finish(pointerId, isBackspace)) return
        if (swipeAction == KeySwipeAction.TOGGLE_LANGUAGE) {
            setLanguage(language.toggled())
            actionDispatcher.toggleLanguage(language)
        } else {
            dispatchTarget(keyId, key, selection, eventTimeMillis)
        }
    }
    fun onAccessibilityClick(keyId: String, eventTimeMillis: Long) {
        if (keyId == ClipboardTargetId) {
            onHapticFeedback(KeyboardHapticType.CONTROL)
            return onClipboardRequested()
        }
        val key = keySpec(keyId) ?: return
        KeyHapticTypeMapper.forTarget(key, isSuggestion = false)?.let(onHapticFeedback)
        dispatchTarget(keyId, key, selection = null, eventTimeMillis)
    }
    fun onAccessibilityAlternate(keyId: String, index: Int) {
        val key = keySpec(keyId) ?: return
        val alternate = key.alternates.getOrNull(index) ?: return
        onHapticFeedback(KeyboardHapticType.CONTROL)
        actionDispatcher.dispatchAlternate(key, alternate)
    }
    fun onAccessibilitySuggestion(targetId: String) {
        val selection = suggestionSelection(targetId) ?: return
        KeyHapticTypeMapper.forTarget(key = null, isSuggestion = true)?.let(onHapticFeedback)
        onSuggestionSelected(selection)
    }
    fun setLanguage(value: KeyboardLanguage) {
        if (language == value) return
        language = value
        onVisualStateChanged()
        onSemanticStateChanged()
    }
    fun setShiftState(value: ShiftState) = actionDispatcher.setShiftState(value)
    fun cancel() {
        alternateSelection.cancelAll()
        backspaceRepeat.cancelAll()
        swipeGestures.cancel()
    }
    fun reset() {
        cancel()
        actionDispatcher.reset()
    }
    private fun dispatchTarget(
        keyId: String?,
        key: KeySpec?,
        selection: SuggestionSelection?,
        eventTimeMillis: Long,
    ) {
        when {
            selection != null -> onSuggestionSelected(selection)
            keyId == ClipboardTargetId -> onClipboardRequested()
            key?.role == KeyRole.SETTINGS -> onSettingsRequested()
            key?.role == KeyRole.CLIPBOARD -> onClipboardPanelRequested()
            key?.role == KeyRole.EMOJI -> onEmojiRequested()
            keyId != null -> actionDispatcher.dispatch(keyId, eventTimeMillis)
        }
    }
}
