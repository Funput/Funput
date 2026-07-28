package app.funput.funput.keyboard.surface

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.accessibility.KeyboardSurfaceAccessibilityController
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
) {
    private val controller = KeyboardSurfaceAccessibilityController(
        host = host,
        activate = ::activate,
        activateAlternate = { keyId, index ->
            interaction().performAccessibilityAlternate(keyId, index)
        },
    )

    fun dispatchHover(event: MotionEvent): Boolean = controller.dispatchHover(event)

    fun refresh() = controller.refresh(keyboard(), shiftState(), suggestions())

    private fun activate(keyId: String) {
        val selection = suggestions().selectionForTarget(keyId)
        if (selection != null) {
            interaction().performAccessibilitySuggestionClick(keyId)
        } else {
            interaction().performAccessibilityClick(keyId, SystemClock.uptimeMillis())
        }
    }
}
