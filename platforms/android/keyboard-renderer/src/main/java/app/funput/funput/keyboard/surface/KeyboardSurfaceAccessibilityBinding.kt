package app.funput.funput.keyboard.surface

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.accessibility.KeyboardSurfaceAccessibilityController
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.R
import app.funput.funput.keyboard.accessibilityLabel
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.ShiftState

/** Keeps virtual-key accessibility wiring out of the keyboard View. */
internal class KeyboardSurfaceAccessibilityBinding(
    host: View,
    private val interaction: () -> KeyboardSurfaceInteraction,
    private val keyboard: () -> ResolvedKeyboard?,
    private val shiftState: () -> ShiftState,
    private val suggestions: () -> List<String>,
    private val clipboardHint: () -> KeyboardClipboardHint?,
) {
    private val resources = host.resources
    private val controller = KeyboardSurfaceAccessibilityController(
        host = host,
        activate = ::activate,
        activateAlternate = { keyId, index ->
            interaction().performAccessibilityAlternate(keyId, index)
        },
    )

    fun dispatchHover(event: MotionEvent): Boolean = controller.dispatchHover(event)

    fun refresh() = controller.refresh(
        keyboard(),
        shiftState(),
        suggestions(),
        clipboardHint().takeIf { suggestions().isEmpty() }?.accessibilityLabel(resources),
        resources.getString(R.string.clipboard_open_accessibility),
    )

    private fun activate(keyId: String) {
        val selection = suggestions().selectionForTarget(keyId)
        if (selection != null) {
            interaction().performAccessibilitySuggestionClick(keyId)
        } else {
            interaction().performAccessibilityClick(keyId, SystemClock.uptimeMillis())
        }
    }
}
