package app.funput.funput.keyboard

internal object SuggestionCapacity {
    fun visibleCount(availableWidthPx: Float, density: Float, requested: Int): Int {
        if (density <= 0f || requested <= 0) return 0
        val capacity = (availableWidthPx / (MinimumSegmentWidthDp * density)).toInt()
        return capacity.coerceIn(0, requested.coerceAtMost(MaxCandidates))
    }

    private const val MinimumSegmentWidthDp = 64f
    private const val MaxCandidates = 3
}
