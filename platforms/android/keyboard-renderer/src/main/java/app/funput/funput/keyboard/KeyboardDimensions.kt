package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.KeyboardSizingProfile
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
    ): Float = baseRecommendedHeightDp(editorMode, profile) * profile.heightScale

    internal fun baseRecommendedHeightDp(
        editorMode: KeyboardEditorMode,
        profile: KeyboardSizingProfile = KeyboardSizingProfile.Default,
    ): Float = when {
        editorMode.usesKeypad -> heightForRowCount(rowCount = 4, hasSuggestionBar = false, profile)
        editorMode.isPassword -> FiveRowHeightDp
        else -> FiveRowWithSuggestionHeightDp
    }

    /** Matches [KeyboardGeometry] row-height cap so keypad rows fill the panel without a blank band. */
    internal fun heightForRowCount(
        rowCount: Int,
        hasSuggestionBar: Boolean,
        profile: KeyboardSizingProfile = KeyboardSizingProfile.Default,
    ): Float {
        require(rowCount > 0) { "Row count must be positive" }
        val contentWidth = DefaultWidthDp - profile.horizontalPaddingDp * 2f
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
        return profile.verticalPaddingDp * 2f + suggestionBarBlock + rowsBlockHeight
    }

    private const val FiveRowWithSuggestionHeightDp = 318f
    private const val FiveRowHeightDp = 252f
}
