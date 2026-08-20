package app.funput.funput.keyboard.accessibility

import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.ShiftState

/** Owns the virtual accessibility tree exposed by a keyboard surface. */
internal class KeyboardSurfaceAccessibilityController(
    host: View,
    activate: (String) -> Unit,
    activateAlternate: (String, Int) -> Unit,
    activateCustom: (Int) -> Unit,
) {
    private var snapshot: KeyboardAccessibilitySnapshot? = null
    private val delegate = KeyboardAccessibilityDelegate(
        host = host,
        snapshot = { snapshot },
        activate = activate,
        activateAlternate = activateAlternate,
        activateCustom = activateCustom,
    )

    fun dispatchHover(event: MotionEvent): Boolean = delegate.dispatchHover(event)

    fun refresh(
        keyboard: ResolvedKeyboard?,
        shiftState: ShiftState,
        suggestions: List<String> = emptyList(),
        clipboardLabel: String? = null,
        clipboardKeyLabel: String? = null,
        smartGesturesEnabled: Boolean = true,
    ) {
        snapshot = keyboard?.let {
            KeyboardAccessibilitySnapshot(
                it, shiftState, suggestions, clipboardLabel, clipboardKeyLabel, smartGesturesEnabled,
            )
        }
        delegate.invalidateRoot()
    }
}
