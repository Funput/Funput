package app.funput.funput.keyboard.touch

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec

/**
 * A two-row keyboard drawn on a 100 × 200 pitch: keys 80 wide with a 20 gap, rows 150 tall
 * with a 50 gap, so one pitch is 100 across and 200 down. `q w e` over `a s d`, plus a
 * VNI tone digit `6` and a non-letter key, which correction must never swap in.
 */
internal object KeyTouchTestKeyboard {
    const val PitchX = 100f
    const val PitchY = 200f

    fun keyboard(pitchX: Float = PitchX, pitchY: Float = PitchY) = ResolvedKeyboard(
        width = 500f,
        height = 400f,
        suggestionBar = null,
        rows = listOf(
            listOf(key("q", 0), key("w", 1), key("e", 2), key("6", 3, KeyRole.VNI_MODIFIER)),
            listOf(key("a", 0, row = 1), key("s", 1, row = 1), key("d", 2, row = 1),
                key("backspace", 3, KeyRole.BACKSPACE, row = 1)),
        ),
        pitchX = pitchX,
        pitchY = pitchY,
    )

    /** The centre of the key in [column] of [row]. */
    fun centre(column: Int, row: Int = 0): Pair<Float, Float> =
        column * PitchX + 40f to row * PitchY + 75f

    private fun key(label: String, column: Int, role: KeyRole = KeyRole.CHARACTER, row: Int = 0): ResolvedKey {
        val left = column * PitchX
        val top = row * PitchY
        return ResolvedKey(
            spec = KeySpec(id = "key-$label", label = label, role = role),
            bounds = KeyBounds(left, top, left + 80f, top + 150f),
        )
    }
}
