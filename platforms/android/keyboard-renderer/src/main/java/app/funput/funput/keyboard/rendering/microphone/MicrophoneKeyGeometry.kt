package app.funput.funput.keyboard.rendering.microphone

import android.graphics.RectF

/** Matches the other toolbar plates without reducing the microphone's reserved touch target. */
internal fun RectF.fitMicrophonePlate() {
    val center = centerX()
    val halfWidth = minOf(width(), height()) / 2f
    left = center - halfWidth
    right = center + halfWidth
}
