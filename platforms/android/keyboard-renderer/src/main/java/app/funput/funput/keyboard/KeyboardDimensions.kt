package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.usesCompactLetterRows
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod

object KeyboardDimensions {
    const val DefaultWidthDp = 360f

    private const val CanonicalColumnCount = 10
    private const val SuggestionBarHeightDp = 42f
    private const val SuggestionBarGapDp = 6f

    fun recommendedHeightDp(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        profile: KeyboardSizingProfile = KeyboardSizingProfile.Default,
        widthDp: Float = DefaultWidthDp,
        showsNumberRow: Boolean = true,
    ): Float = baseRecommendedHeightDp(editorMode, profile, widthDp, inputMethod, showsNumberRow)

    internal fun baseRecommendedHeightDp(
        editorMode: KeyboardEditorMode,
        profile: KeyboardSizingProfile = KeyboardSizingProfile.Default,
        widthDp: Float = DefaultWidthDp,
        inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX,
        showsNumberRow: Boolean = true,
    ): Float = when {
        editorMode == KeyboardEditorMode.PIN -> heightForRowCount(4, false, profile, widthDp)
        editorMode.usesKeypad -> heightForRowCount(4, true, profile, widthDp)
        editorMode.isPassword -> heightForRowCount(5, false, profile, widthDp)
        else -> {
            val compact = usesCompactLetterRows(inputMethod, editorMode, showsNumberRow)
            heightForRowCount(if (compact) 4 else 5, true, profile, widthDp)
        }
    }

    /**
     * Matches [KeyboardGeometry] row-height cap so keypad rows fill the panel without a blank band.
     * The whole panel scales by [KeyboardSizingProfile.heightScale]; the geometry spec scales its
     * vertical dimensions by the same factor so the rows still fill it exactly.
     */
    internal fun heightForRowCount(
        rowCount: Int,
        hasSuggestionBar: Boolean,
        profile: KeyboardSizingProfile = KeyboardSizingProfile.Default,
        widthDp: Float = DefaultWidthDp,
    ): Float {
        require(rowCount > 0) { "Row count must be positive" }
        val contentWidth = minOf(widthDp, DefaultWidthDp) - profile.horizontalPaddingDp * 2f
        require(contentWidth > 0f) { "Keyboard width must exceed its horizontal padding" }
        val canonicalUnit = contentWidth /
            (CanonicalColumnCount + (CanonicalColumnCount - 1) * profile.horizontalGapRatio)
        val rowHeight = canonicalUnit / profile.keyAspectRatio
        val verticalGap = rowHeight * profile.verticalGapRatio
        val rowsBlockHeight = rowCount * rowHeight + (rowCount - 1) * verticalGap
        val suggestionBarBlock = if (hasSuggestionBar) {
            SuggestionBarHeightDp + SuggestionBarGapDp
        } else {
            0f
        }
        return (profile.verticalPaddingDp * 2f + suggestionBarBlock + rowsBlockHeight) * profile.heightScale
    }
}
