package app.funput.funput.keyboard.ui.host

import android.view.View
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.ui.KeyboardSafeAreaController
import app.funput.funput.keyboard.ui.placement.KeyboardPlacementHostController
import kotlin.math.roundToInt

internal data class KeyboardHostSize(val widthSpec: Int, val heightSpec: Int)

/** Resolves host constraints and placement in the same order as the surface measurement. */
internal class KeyboardHostMeasure(
    private val host: View,
    private val safeArea: KeyboardSafeAreaController,
    private val placement: KeyboardPlacementHostController,
) {
    fun resolve(
        widthSpec: Int,
        heightSpec: Int,
        method: KeyboardInputMethod,
        editor: KeyboardEditorMode,
        profile: KeyboardSizingProfile,
        numberRow: Boolean,
    ): KeyboardHostSize {
        val density = host.resources.displayMetrics.density
        val defaultWidth = (KeyboardDimensions.DefaultWidthDp * density).roundToInt()
        val width = View.resolveSize(defaultWidth + safeArea.horizontalInset, widthSpec)
        val contentWidth = placement.resolveContentWidth(width - safeArea.horizontalInset)
        val heightDp = KeyboardDimensions.recommendedHeightDp(
            method, editor, profile, contentWidth / density, numberRow,
        )
        val height = placement.resolveHeight((heightDp * density).roundToInt(), heightSpec)
        return KeyboardHostSize(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
    }
}
