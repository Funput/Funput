package app.funput.funput.keyboard

import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod

object KeyboardDimensions {
    const val DefaultWidthDp = 360f

    fun recommendedHeightDp(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
    ): Float = when {
        editorMode.usesKeypad -> 300f
        editorMode.isPassword -> 348f
        editorMode.supportsVietnameseComposition && inputMethod == KeyboardInputMethod.VNI -> 348f
        else -> 290f
    }
}
