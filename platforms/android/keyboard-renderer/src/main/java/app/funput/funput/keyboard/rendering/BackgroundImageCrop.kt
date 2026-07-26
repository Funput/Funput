package app.funput.funput.keyboard.rendering

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import kotlin.math.roundToInt

/**
 * A region of the source image, in pixels.
 *
 * Deliberately not `android.graphics.Rect`: that class is stubbed in JVM unit tests, where every
 * field silently reads back as zero, so geometry returned as a `Rect` cannot be asserted on.
 */
internal data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val centerX get() = (left + right) / 2
}

/**
 * Which part of the image the keyboard shows.
 *
 * Pure geometry with no Android drawing, so the framing rules can be tested directly. The crop is
 * aspect-fill first — the image always covers the keyboard, never letterboxes — then narrowed by
 * zoom and slid to the focal point. The focal point is clamped last so that pushing it to an edge
 * stops at the image boundary instead of exposing blank space.
 */
internal object BackgroundImageCrop {
    fun sourceRect(
        imageWidth: Int,
        imageHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        framing: KeyboardThemeBackgroundImage,
    ): CropRect {
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val fillWidth: Float
        val fillHeight: Float
        if (imageRatio > targetRatio) {
            fillHeight = imageHeight.toFloat()
            fillWidth = fillHeight * targetRatio
        } else {
            fillWidth = imageWidth.toFloat()
            fillHeight = fillWidth / targetRatio
        }

        val zoom = framing.zoom.coerceAtLeast(KeyboardThemeBackgroundImage.MinZoom)
        val cropWidth = fillWidth / zoom
        val cropHeight = fillHeight / zoom
        val left = (framing.focalX * imageWidth - cropWidth / 2f)
            .coerceIn(0f, (imageWidth - cropWidth).coerceAtLeast(0f))
        val top = (framing.focalY * imageHeight - cropHeight / 2f)
            .coerceIn(0f, (imageHeight - cropHeight).coerceAtLeast(0f))

        return CropRect(
            left.roundToInt(),
            top.roundToInt(),
            (left + cropWidth).roundToInt().coerceAtMost(imageWidth),
            (top + cropHeight).roundToInt().coerceAtMost(imageHeight),
        )
    }
}
