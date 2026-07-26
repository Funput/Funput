package app.funput.funput.keyboard.ui.emoji

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

internal fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

internal fun surface(color: Int, radiusDp: Int, view: View) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = view.dp(radiusDp).toFloat()
}

internal fun TextView.configureAction(label: String) {
    text = label
    gravity = android.view.Gravity.CENTER
    textSize = 15f
    minWidth = dp(44)
    minHeight = dp(44)
    isClickable = true
    isFocusable = true
}
