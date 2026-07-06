package app.funput.funput.keyboard.surface

import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.interaction.KeyboardSurfaceInteraction
import app.funput.funput.keyboard.interaction.KeyboardTouchHandler

/** Routes input events and applies the interactive or read-only surface policy. */
internal class KeyboardSurfaceEventDispatcher(
    private val host: View,
    private val interaction: KeyboardSurfaceInteraction,
    private val dispatchAccessibilityHover: (MotionEvent) -> Boolean,
) {
    var enabled: Boolean = true
        private set

    init {
        updateHostState()
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        updateHostState()
        if (!value) interaction.clear()
    }

    fun dispatchTouch(event: MotionEvent, performClick: () -> Boolean): Boolean {
        if (!enabled) return false
        return when (interaction.onTouchEvent(event)) {
            KeyboardTouchHandler.Result.UNHANDLED -> false
            KeyboardTouchHandler.Result.HANDLED -> true
            KeyboardTouchHandler.Result.CLICK -> performClick()
        }
    }

    fun dispatchHover(event: MotionEvent, fallback: () -> Boolean): Boolean {
        if (!enabled) return fallback()
        return dispatchAccessibilityHover(event) || fallback()
    }

    private fun updateHostState() {
        host.isClickable = enabled
        host.importantForAccessibility = if (enabled) {
            View.IMPORTANT_FOR_ACCESSIBILITY_YES
        } else {
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }
}
