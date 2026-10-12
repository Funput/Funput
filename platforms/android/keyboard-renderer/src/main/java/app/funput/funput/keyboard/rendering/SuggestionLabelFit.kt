package app.funput.funput.keyboard.rendering

/**
 * How a suggestion is squeezed into its slot of the bar.
 *
 * Candidates share the prefix being typed, so the part that tells them apart is the end. Cutting
 * every long one at the end left a bar of identical "Autom…" (issue #546). A long word is instead
 * made smaller first, which keeps the letter shapes, then narrowed horizontally, the way Gboard
 * fits "Automatically". Only a word too long for both is shortened, and then in the middle, so the
 * distinguishing ending still shows.
 */
internal data class SuggestionLabelFit(
    val sizeFactor: Float,
    val scaleX: Float,
    val ellipsize: Boolean,
) {
    companion object {
        /** Smallest text size, as a share of the normal one, before narrowing begins. */
        const val MinSizeFactor = 0.8f

        /** Narrowest horizontal scale; below it letters stop reading as letters. */
        const val MinScaleX = 0.7f

        private val Unchanged = SuggestionLabelFit(1f, 1f, ellipsize = false)

        /** [textWidth] is measured at the normal size with no narrowing. */
        fun fit(textWidth: Float, availableWidth: Float): SuggestionLabelFit {
            if (textWidth <= availableWidth || availableWidth <= 0f) return Unchanged
            val sizeFactor = maxOf(MinSizeFactor, availableWidth / textWidth)
            val sizedWidth = textWidth * sizeFactor
            if (sizedWidth <= availableWidth) return SuggestionLabelFit(sizeFactor, 1f, ellipsize = false)
            val scaleX = maxOf(MinScaleX, availableWidth / sizedWidth)
            return SuggestionLabelFit(sizeFactor, scaleX, ellipsize = sizedWidth * scaleX > availableWidth)
        }
    }
}
