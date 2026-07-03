package app.funput.funput.keyboard

import app.funput.funput.keyboard.model.KeyboardInputMethod

internal object KeyboardDimensions {
    const val DefaultWidthDp = 360f

    fun recommendedHeightDp(inputMethod: KeyboardInputMethod): Float = when (inputMethod) {
        KeyboardInputMethod.TELEX -> 290f
        KeyboardInputMethod.VNI -> 348f
    }
}
