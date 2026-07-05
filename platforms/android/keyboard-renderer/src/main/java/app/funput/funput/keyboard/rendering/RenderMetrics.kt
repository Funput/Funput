package app.funput.funput.keyboard.rendering

import android.content.res.Resources

internal class RenderMetrics(private val resources: Resources) {
    private val density: Float = resources.displayMetrics.density
    private var labelScale: Float = 1f

    fun updateLabelScale(scale: Float) {
        require(scale > 0f) { "Label scale must be positive" }
        labelScale = scale
    }

    fun dp(value: Float): Float = value * density

    fun sp(value: Float): Float = value * density *
        resources.configuration.fontScale.coerceAtMost(MaxFontScale) * labelScale

    private companion object {
        const val MaxFontScale = 1.25f
    }
}
