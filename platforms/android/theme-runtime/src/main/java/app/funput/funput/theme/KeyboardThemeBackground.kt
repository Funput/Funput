package app.funput.funput.theme

/**
 * Optional background layer for custom themes.
 *
 * The source is intentionally a string so app storage can use a content URI, file URI, or stable
 * theme-package asset reference later without making the renderer depend on Android storage APIs.
 *
 * Framing is stored rather than baked into the file so the user can re-crop without the image
 * degrading through repeated re-encoding, and so the same asset can be reframed for a different
 * keyboard height later.
 */
data class KeyboardThemeBackgroundImage(
    val source: String,
    val opacity: Float,
    /** Which point of the image to keep centred, in image fractions. */
    val focalX: Float = CenterFocus,
    val focalY: Float = CenterFocus,
    /** How far in to crop; 1 fills the keyboard with the whole image. */
    val zoom: Float = MinZoom,
    /** Softens the image so key labels stay readable over it. */
    val blurRadiusDp: Float = 0f,
    /** Wash drawn over the image, the usual way to calm a busy photo. */
    val overlayColor: Int = Transparent,
) {
    init {
        require(source.isNotBlank()) { "Background image source must not be blank" }
        require(opacity in 0f..1f) { "Background image opacity must be between 0 and 1" }
        require(focalX in FocusRange) { "Focal X must be within $FocusRange" }
        require(focalY in FocusRange) { "Focal Y must be within $FocusRange" }
        require(zoom in ZoomRange) { "Zoom must be within $ZoomRange" }
        require(blurRadiusDp in BlurRange) { "Blur radius must be within $BlurRange" }
    }

    companion object {
        const val CenterFocus = 0.5f
        const val MinZoom = 1f
        const val Transparent = 0x00000000

        val FocusRange = 0f..1f
        val ZoomRange = MinZoom..4f
        val BlurRange = 0f..24f
    }
}
