package app.funput.funput.keyboard.surface

import android.view.View
import android.view.ViewConfiguration
import app.funput.funput.keyboard.KeyboardCallbacks
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.layout.KeyBounds

/** Creates the Android-bound interaction pipeline shared by live and preview surfaces. */
internal fun createKeyboardSurfaceInteraction(
    host: View,
    callbacks: KeyboardCallbacks,
    keyAt: (x: Float, y: Float) -> String?,
    keySpec: (keyId: String) -> KeySpec?,
    suggestionSelection: (targetId: String) -> SuggestionSelection?,
    onHapticFeedback: (KeyboardHapticType) -> Unit,
    onVisualStateChanged: () -> Unit,
    onSemanticStateChanged: () -> Unit,
    keyBounds: (keyId: String) -> KeyBounds?,
    surfaceBounds: () -> KeyBounds,
): KeyboardSurfaceInteraction = KeyboardSurfaceInteraction(
    keyAt = keyAt,
    keySpec = keySpec,
    suggestionSelection = suggestionSelection,
    onAction = callbacks::dispatch,
    onEmojiRequested = callbacks::dispatchEmojiRequest,
    onClipboardPanelRequested = callbacks::dispatchClipboardPanelRequest,
    onClipboardPasteRequested = callbacks::dispatchClipboardPasteRequest,
    onSuggestionSelected = callbacks::dispatchSuggestion,
    onHapticFeedback = onHapticFeedback,
    onVisualStateChanged = onVisualStateChanged,
    onSemanticStateChanged = onSemanticStateChanged,
    schedule = { task, delay -> host.postDelayed(task, delay) },
    cancel = host::removeCallbacks,
    requestParentIntercept = { disallow -> host.parent?.requestDisallowInterceptTouchEvent(disallow) },
    doubleTapTimeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong(),
    density = host.resources.displayMetrics.density,
    touchSlop = ViewConfiguration.get(host.context).scaledTouchSlop.toFloat(),
    keyBounds = keyBounds,
    surfaceBounds = surfaceBounds,
)
