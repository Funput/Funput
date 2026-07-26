package app.funput.funput.keyboard.rendering

import android.content.res.Resources
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import app.funput.funput.keyboard.R
import app.funput.funput.keyboard.layout.KeyBounds
import kotlin.math.roundToInt

internal class ToolbarLogoRenderer(private val resources: Resources) {
    private var logoDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_funput_toolbar_logo, null)?.mutate()

    fun draw(canvas: Canvas, bounds: KeyBounds) {
        val drawable = logoDrawable ?: return
        drawable.setBounds(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt(),
        )
        drawable.draw(canvas)
    }
}
