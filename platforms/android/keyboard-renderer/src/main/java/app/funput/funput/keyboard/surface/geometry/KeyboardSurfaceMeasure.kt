package app.funput.funput.keyboard.surface.geometry

import android.view.View
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import kotlin.math.roundToInt

internal data class KeyboardSurfaceSize(val width: Int, val height: Int)

internal fun measureKeyboardSurface(
    host: View,
    widthSpec: Int,
    heightSpec: Int,
    method: KeyboardInputMethod,
    editor: KeyboardEditorMode,
    profile: KeyboardSizingProfile,
    numberRow: Boolean,
): KeyboardSurfaceSize {
    val density = host.resources.displayMetrics.density
    val width = View.resolveSize((KeyboardDimensions.DefaultWidthDp * density).roundToInt(), widthSpec)
    val heightDp = KeyboardDimensions.recommendedHeightDp(
        method, editor, profile, width / density, numberRow,
    )
    return KeyboardSurfaceSize(width, View.resolveSize((heightDp * density).roundToInt(), heightSpec))
}
