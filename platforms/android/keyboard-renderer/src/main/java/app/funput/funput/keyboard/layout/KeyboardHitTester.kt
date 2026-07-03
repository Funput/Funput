package app.funput.funput.keyboard.layout

/** Allocation-free lookup over precomputed key hit targets. */
internal class KeyboardHitTester(
    private val width: Float,
    private val height: Float,
    private val keys: List<ResolvedKey>,
) {
    fun keyAt(x: Float, y: Float): ResolvedKey? {
        if (x < 0f || x > width || y < 0f || y > height) return null
        for (key in keys) {
            if (key.hitBounds.contains(x, y)) return key
        }
        return null
    }
}
