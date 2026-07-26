package app.funput.funput.keyboard.background

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlin.math.roundToInt

/**
 * Softens a background image.
 *
 * Implemented by downscaling and scaling back with bilinear filtering rather than a true Gaussian.
 * RenderScript is removed, and `RenderEffect` needs API 31 while this module supports 26, so a
 * real blur would mean two code paths and a hard visual difference between devices. For a
 * background sitting behind key labels the approximation is indistinguishable, and it costs one
 * allocation instead of a per-frame GPU pass.
 */
internal object BitmapBlur {
    fun applied(source: Bitmap, radiusDp: Float, density: Float): Bitmap {
        val radiusPx = radiusDp * density
        if (radiusPx < MinimumVisibleRadiusPx) return source

        // Each halving of size is worth roughly one pixel of blur radius at full size.
        val factor = (radiusPx / RadiusPerStep).coerceIn(MinFactor, MaxFactor)
        val width = (source.width / factor).roundToInt().coerceAtLeast(MinDimension)
        val height = (source.height / factor).roundToInt().coerceAtLeast(MinDimension)
        val small = source.scale(width, height)
        val blurred = small.scale(source.width, source.height)
        if (small !== blurred) small.recycle()
        return blurred
    }

    private const val MinimumVisibleRadiusPx = 1f
    private const val RadiusPerStep = 1.5f
    private const val MinFactor = 1.5f
    private const val MaxFactor = 32f
    private const val MinDimension = 2
}
