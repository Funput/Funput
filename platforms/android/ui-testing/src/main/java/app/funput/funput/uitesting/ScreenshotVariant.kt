package app.funput.funput.uitesting

/**
 * One rendering condition a screen is captured under: light or dark, at one font scale.
 *
 * Kept free of any app type (the app's own appearance enum lives in `:ime`), so every module can
 * use it; a test maps [isDark] onto whatever its theme expects.
 */
data class ScreenshotVariant(
    /** Whether the system is in night mode. */
    val isDark: Boolean,
    /** The system font scale, 1.0 being the default size. */
    val fontScale: Float,
) {
    /** A stable file-name fragment, e.g. `dark-font130`. */
    val fileName: String
        get() = "${if (isDark) "dark" else "light"}-font${(fontScale * 100).toInt()}"

    /** Readable form for parameterized test names. */
    override fun toString(): String = fileName

    companion object {
        /** Font scales every screen is checked at: default, and the common "large" setting. */
        val FontScales: List<Float> = listOf(1.0f, 1.3f)

        /** Every combination of appearance and [FontScales]. */
        val All: List<ScreenshotVariant> = listOf(false, true).flatMap { dark ->
            FontScales.map { scale -> ScreenshotVariant(isDark = dark, fontScale = scale) }
        }

        /**
         * [All], shaped for Robolectric's `@ParameterizedRobolectricTestRunner.Parameters`, which
         * expects one argument array per case.
         */
        fun parameters(): List<Array<Any>> = All.map { variant -> arrayOf(variant) }
    }
}
