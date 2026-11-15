package app.funput.funput.keyboard.surface

import app.funput.funput.keyboard.suggestion.SuggestionChip
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.accessibility.ClipboardAccessibility
import app.funput.funput.keyboard.accessibility.KeyboardSurfaceAccessibilityController
import app.funput.funput.keyboard.accessibility.SmartGestureAccessibility
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.R
import app.funput.funput.keyboard.accessibilityLabel
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.suggestion.actions.SuggestionAction

/** Keeps virtual-key accessibility wiring out of the keyboard View. */
internal class KeyboardSurfaceAccessibilityBinding(
    host: View,
    private val interaction: () -> KeyboardSurfaceInteraction,
    private val keyboard: () -> ResolvedKeyboard?,
    private val shiftState: () -> ShiftState,
    private val suggestions: () -> List<SuggestionChip>,
    private val clipboardHint: () -> KeyboardClipboardHint?,
) {
    private val resources = host.resources
    private val controller = KeyboardSurfaceAccessibilityController(
        host = host,
        activate = ::activate,
        activateAlternate = { keyId, index ->
            interaction().performAccessibilityAlternate(keyId, index)
        },
        activateCustom = { keyId, actionId ->
            when {
                ClipboardAccessibility.isOpenHistory(actionId) ->
                    interaction().performAccessibilityClipboardPanel()
                else -> {
                    val suggestion = SuggestionAction.forAccessibilityAction(actionId)
                    if (suggestion != null) {
                        interaction().performAccessibilitySuggestionAction(keyId, suggestion)
                    } else {
                        SmartGestureAccessibility.keyAction(actionId)
                            ?.let { interaction().performAccessibilityAction(it) }
                    }
                }
            }
        },
    )

    fun dispatchHover(event: MotionEvent): Boolean = controller.dispatchHover(event)

    fun refresh() = controller.refresh(
        keyboard(),
        shiftState(),
        suggestions(),
        clipboardKeyLabel(),
        interaction().areSmartGesturesEnabled,
        interaction().breaksLineOnEnterHold,
        interaction()::isSuggestionEditable,
    )

    private fun clipboardKeyLabel(): String {
        val hint = clipboardHint()
        return if (hint != null) {
            hint.accessibilityLabel(resources)
        } else {
            resources.getString(R.string.clipboard_open_accessibility)
        }
    }

    private fun activate(keyId: String) {
        val selection = suggestions().selectionForTarget(keyId)
        if (selection != null) {
            interaction().performAccessibilitySuggestionClick(keyId)
        } else {
            interaction().performAccessibilityClick(keyId, SystemClock.uptimeMillis())
        }
    }
}
