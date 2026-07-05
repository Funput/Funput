package app.funput.funput.keyboard

import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod

object KeyboardDimensions {
    const val DefaultWidthDp = 360f

    fun recommendedHeightDp(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        profile: KeyboardSizingProfile = KeyboardSizingProfile.Default,
    ): Float = baseRecommendedHeightDp(inputMethod, editorMode) * profile.heightScale

    internal fun baseRecommendedHeightDp(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
    ): Float = when {
        editorMode.usesKeypad -> KeypadHeightDp
        editorMode.isPassword -> FiveRowHeightDp
        editorMode.supportsVietnameseComposition && inputMethod == KeyboardInputMethod.VNI ->
            FiveRowWithSuggestionHeightDp
        else -> FourRowWithSuggestionHeightDp
    }

    private const val FourRowWithSuggestionHeightDp = 268f
    private const val FiveRowWithSuggestionHeightDp = 318f
    private const val FiveRowHeightDp = 252f
    private const val KeypadHeightDp = 300f
}
