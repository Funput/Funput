package app.funput.funput.ime.editing.keyevent

/**
 * The same prefix arithmetic as committed replacement, without touching InputConnection.
 *
 * Grow by appending the suffix, shrink by deleting the tail, and rewrite by delete-then-insert
 * when a tone transform changes a character in the middle.
 */
internal data class KeyEventTextPlan(val deleteCount: Int, val insert: String) {
    fun strokes(): List<KeyEventStroke> = buildList {
        repeat(deleteCount) { add(KeyEventStroke.Delete) }
        if (insert.isNotEmpty()) add(KeyEventStroke.Text(insert))
    }

    companion object {
        fun replace(previous: String, replacement: String): KeyEventTextPlan = when {
            previous == replacement -> KeyEventTextPlan(0, "")
            previous.isEmpty() -> KeyEventTextPlan(0, replacement)
            replacement.startsWith(previous) ->
                KeyEventTextPlan(0, replacement.substring(previous.length))
            previous.startsWith(replacement) ->
                KeyEventTextPlan(previous.length - replacement.length, "")
            else -> KeyEventTextPlan(previous.length, replacement)
        }
    }
}
