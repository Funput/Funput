package app.funput.funput.keyboard.touch

import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyAction
import java.util.Locale

/**
 * Remembers where each pointer came down and attaches that point, as touch evidence, to the
 * letter it types when it lifts.
 *
 * The point is the touch-down one, not the lift: a fast typist's finger rolls as it leaves
 * the glass, and where it landed is where it was aiming. Only an action emitted while a real
 * lift is being handled, for the key under that pointer, with the key's own letter, gets
 * evidence. An alternate picked from a popup, an accessibility click and a repeated key all
 * reach the host without one, and the engine falls back to its static layout for them.
 */
internal class KeyTouchRecorder(private val keyboard: () -> ResolvedKeyboard?) {
    private val downs = HashMap<Int, Pair<Float, Float>>()
    private var releasing: Releasing? = null

    fun down(pointerId: Int, x: Float, y: Float) {
        downs[pointerId] = x to y
    }

    /** Runs [release] with [pointerId]'s touch-down point available to [attach]. */
    fun releasing(pointerId: Int, keyId: String?, release: () -> Unit) {
        val point = downs.remove(pointerId)
        releasing = if (point != null && keyId != null) Releasing(keyId, point.first, point.second) else null
        try {
            release()
        } finally {
            releasing = null
        }
    }

    fun attach(action: KeyAction): KeyAction {
        if (action !is KeyAction.Input) return action
        val lift = releasing?.takeIf { it.keyId == action.keyId } ?: return action
        val resolved = keyboard() ?: return action
        val key = resolved.rows.firstNotNullOfOrNull { row -> row.firstOrNull { it.spec.id == lift.keyId } }
        if (key == null || action.text.lowercase(Locale.ROOT) != key.spec.label.lowercase(Locale.ROOT)) {
            return action
        }
        val evidence = KeyTouchEvidenceResolver.resolve(resolved, lift.x, lift.y, lift.keyId) ?: return action
        return action.copy(touch = evidence)
    }

    fun clear() {
        downs.clear()
        releasing = null
    }

    private data class Releasing(val keyId: String, val x: Float, val y: Float)
}
