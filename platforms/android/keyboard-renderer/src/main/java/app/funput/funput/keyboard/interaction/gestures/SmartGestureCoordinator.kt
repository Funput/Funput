package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.interaction.gestures.cursor.SpaceCursorPanTracker
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec

/**
 * Turns space-hold pans and backspace rubs into semantic keyboard actions.
 *
 * Claiming captures the pointer so a neighbouring key cannot commit on release.
 */
internal class SmartGestureCoordinator(
    private val density: Float,
    schedule: (Runnable, Long) -> Unit,
    cancel: (Runnable) -> Unit,
    private val onAction: (KeyAction) -> Unit,
    private val onHaptic: (KeyboardHapticType) -> Unit,
    private val onCaptured: (Int) -> Unit,
    private val hasRepeated: () -> Boolean,
    private val stopRepeat: (Int) -> Unit,
) {
    var enabled = true
    private val sessions = mutableMapOf<Int, SmartGestureSession>()
    private val hold = KeyHoldController(schedule, cancel) { pointerId ->
        sessions[pointerId]?.holdArmed = true
    }

    fun isCaptured(pointerId: Int): Boolean = sessions[pointerId]?.claimed == true

    fun onStarted(pointerId: Int, key: KeySpec?, x: Float, y: Float) {
        if (key == null) return
        sessions[pointerId] = SmartGestureSession(key, x, y, enabled, density)
        if (enabled && key.role == KeyRole.SPACE) hold.start(pointerId)
    }

    /** Returns true when this contact is fully owned by a gesture. */
    fun onMoved(pointerId: Int, x: Float, y: Float): Boolean {
        val session = sessions[pointerId] ?: return false
        session.moveTo(x, y)
        if (session.trackpad != null) {
            emitTrackpad(session)
            return true
        }
        if (session.ratchet != null) {
            emitRatchet(session)
            return true
        }
        if (session.hasWandered && !session.holdArmed) hold.cancel(pointerId)
        if (claimTrackpad(pointerId, session)) {
            emitTrackpad(session)
            return true
        }
        if (claimRatchet(pointerId, session)) {
            emitRatchet(session)
            return true
        }
        return false
    }

    /** Returns true when release must not emit the ordinary key action. */
    fun onReleased(pointerId: Int): Boolean {
        hold.cancel(pointerId)
        val session = sessions.remove(pointerId) ?: return false
        val ratchet = session.ratchet ?: return session.trackpad != null
        if (!ratchet.hasDeleted) onAction(KeyAction.Backspace)
        return true
    }

    fun cancelAll() {
        hold.cancelAll()
        sessions.clear()
    }

    fun forget(pointerId: Int) {
        hold.cancel(pointerId)
        sessions.remove(pointerId)
    }

    private fun claimTrackpad(pointerId: Int, session: SmartGestureSession): Boolean {
        if (session.initialKey.role != KeyRole.SPACE || !session.trackpadReady()) return false
        if (hasRepeated()) return false
        detach(pointerId)
        session.trackpad = SpaceCursorPanTracker(session.stepWidthPx, session.stepHeightPx)
        onHaptic(KeyboardHapticType.CONTROL)
        return true
    }

    private fun claimRatchet(pointerId: Int, session: SmartGestureSession): Boolean {
        if (!session.smartGestures || session.initialKey.role != KeyRole.BACKSPACE) return false
        if (hasRepeated()) return false
        val ratchet = BackspaceWordRatchet(session.ratchetActivationPx, step = session.ratchetStepPx)
        if (!ratchet.shouldClaim(session.translationX, session.translationY)) return false
        detach(pointerId)
        session.ratchet = ratchet
        return true
    }

    private fun emitTrackpad(session: SmartGestureSession) {
        val step = session.trackpad
            ?.update(session.translationX, session.translationY)
            ?: return
        if (step.isEmpty) return
        onAction(KeyAction.MoveCursor(step.columns, step.lines))
        // A line change moves the caret further than the user can track from the finger alone, so
        // it gets the firmer tick and stays distinguishable without looking.
        onHaptic(
            if (step.lines == 0) KeyboardHapticType.DELETE_REPEAT else KeyboardHapticType.CONTROL,
        )
    }

    private fun emitRatchet(session: SmartGestureSession) {
        val words = session.ratchet?.update(session.translationX) ?: return
        repeat(words) { onAction(KeyAction.DeleteWord) }
        if (words > 0) onHaptic(KeyboardHapticType.DELETE)
    }

    private fun detach(pointerId: Int) {
        stopRepeat(pointerId)
        onCaptured(pointerId)
    }
}
