package app.funput.funput.theme

import kotlin.math.abs

/**
 * The colours an image is mostly made of, ranked by how much of it they cover.
 *
 * Pixels are bucketed by hue rather than clustered in RGB. A photograph's RGB clusters are mostly
 * the same subject at different exposures, so clustering hands back five shades of one thing; what
 * a theme needs is the handful of *different* colours in the picture. Buckets also make this
 * deterministic, which is what lets it be tested against a picture built by hand.
 *
 * Greys, near-blacks and near-whites are skipped: they carry no hue to dye a theme with. An image
 * that has nothing else — a black-and-white photograph — returns nothing rather than a grey that
 * would look like the picker had failed to notice the colour.
 */
object DominantColors {

    fun extract(pixels: IntArray, limit: Int = 5): List<Int> {
        val buckets = HashMap<Int, Bucket>()
        pixels.forEach { pixel ->
            if ((pixel ushr 24) < MinAlpha) return@forEach
            val hsl = pixel.toHsl()
            if (hsl.saturation < MinSaturation) return@forEach
            if (hsl.lightness < MinLightness || hsl.lightness > MaxLightness) return@forEach
            buckets.getOrPut((hsl.hue / BucketDegrees).toInt()) { Bucket() }.add(pixel)
        }
        return buckets.values
            .sortedByDescending(Bucket::count)
            .map(Bucket::average)
            .fold(mutableListOf<Int>()) { kept, colour ->
                // Two buckets either side of a boundary are the same colour to the eye, and a row
                // of near-identical swatches is a row of one choice.
                if (kept.none { it.hueDistanceTo(colour) < MinHueSeparation }) kept.add(colour)
                kept
            }
            .take(limit)
    }

    private class Bucket {
        var count = 0
            private set
        private var red = 0L
        private var green = 0L
        private var blue = 0L

        fun add(pixel: Int) {
            count++
            red += (pixel shr 16) and Mask
            green += (pixel shr 8) and Mask
            blue += pixel and Mask
        }

        fun average(): Int = (0xFF shl 24) or
            ((red / count).toInt() shl 16) or
            ((green / count).toInt() shl 8) or
            (blue / count).toInt()
    }

    private fun Int.hueDistanceTo(other: Int): Float {
        val difference = abs(hueDegrees() - other.hueDegrees())
        return minOf(difference, 360f - difference)
    }

    private const val Mask = 0xFF
    private const val MinAlpha = 128
    private const val MinSaturation = 0.18f
    private const val MinLightness = 0.12f
    private const val MaxLightness = 0.92f
    private const val BucketDegrees = 15f
    private const val MinHueSeparation = 25f
}
