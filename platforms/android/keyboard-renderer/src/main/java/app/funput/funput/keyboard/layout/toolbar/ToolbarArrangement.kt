package app.funput.funput.keyboard.layout.toolbar

/**
 * Ordered toolbar sides. Both [leading] and [trailing] are left-to-right: first entry is the
 * leftmost key on that side, last entry is the rightmost (flush with the side's outer edge).
 */
data class ToolbarArrangement(
    val leading: List<ToolbarSlot>,
    val trailing: List<ToolbarSlot>,
) {
    /**
     * Drops unavailable slots, keeps the first occurrence of each role, and restores any missing
     * available role onto its default side so the bar stays complete.
     */
    fun normalize(available: Set<ToolbarSlot>): ToolbarArrangement {
        val seen = linkedSetOf<ToolbarSlot>()
        fun take(source: List<ToolbarSlot>) = source.filter { it in available && seen.add(it) }
        val nextLeading = take(leading).toMutableList()
        val nextTrailing = take(trailing).toMutableList()
        available.forEach { slot ->
            if (!seen.add(slot)) return@forEach
            if (slot in Default.leading) nextLeading += slot else nextTrailing += slot
        }
        return ToolbarArrangement(nextLeading, nextTrailing)
    }

    companion object {
        /** Matches today's fixed toolbar: mic and placement on the left, emoji outermost right. */
        val Default = ToolbarArrangement(
            leading = listOf(ToolbarSlot.MICROPHONE, ToolbarSlot.PLACEMENT),
            trailing = listOf(ToolbarSlot.CLIPBOARD, ToolbarSlot.EMOJI),
        )
    }
}
