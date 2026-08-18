package app.funput.funput.keyboard.layout

/** Density-independent keyboard geometry, scaled by a single user-chosen factor. */
data class KeyboardSizingProfile(
    val horizontalPaddingDp: Float = 6f,
    val verticalPaddingDp: Float = 6f,
    val horizontalGapRatio: Float = 0.11f,
    val verticalGapRatio: Float = 0.16f,
    val keyAspectRatio: Float = DefaultKeyAspectRatio,
    val heightScale: Float = 1f,
    val labelScale: Float = 0.96f,
) {
    init {
        require(horizontalPaddingDp >= 0f) { "Horizontal padding must not be negative" }
        require(verticalPaddingDp >= 0f) { "Vertical padding must not be negative" }
        require(horizontalGapRatio >= 0f) { "Horizontal gap ratio must not be negative" }
        require(verticalGapRatio >= 0f) { "Vertical gap ratio must not be negative" }
        require(keyAspectRatio > 0f) { "Key aspect ratio must be positive" }
        require(heightScale > 0f) { "Height scale must be positive" }
        require(labelScale > 0f) { "Label scale must be positive" }
    }

    companion object {
        /**
         * Key width over row pitch, calibrated so 100% lands on Gboard's untouched default.
         *
         * Measured on a 1440x3200 phone at density 3.75 (384dp wide): Gboard rows sit on a 189.5px
         * pitch, and at the previous 0.75 this keyboard produced 184px — about 3% short, which read
         * as Funput being the smaller keyboard at the same nominal size. The slider still spans
         * [MinScale]..[MaxScale] around this baseline.
         */
        const val DefaultKeyAspectRatio = 0.73f

        /** The range Settings offers, matching the iOS slider so the two platforms agree. */
        const val MinScale = 0.85f
        const val MaxScale = 1.2f
        const val DefaultScale = 1.08f

        /**
         * The tallest a keyboard is allowed to grow in landscape on a phone.
         *
         * A phone rotated on its side has far less height to spend than it does upright, so the
         * ceiling here sits below even the old fixed default ("Large", 1.08) — the old default
         * was tuned for portrait and was already tall for a landscape phone strip. Tablets keep
         * their full landscape height because they have the height to spare; see
         * [constrainedForLandscape].
         */
        const val PhoneLandscapeMaxScale = 0.92f

        val Normal = scaled(1f)

        val Default: KeyboardSizingProfile = scaled(DefaultScale)

        /**
         * The profile for a height scale, clamped to the offered range.
         *
         * Labels follow the keys one for one: a key that grows by 8% with text that stays put
         * reads as a key with too much padding rather than as a larger key.
         */
        fun scaled(scale: Float): KeyboardSizingProfile {
            val clamped = scale.coerceIn(MinScale, MaxScale)
            return KeyboardSizingProfile(
                heightScale = clamped,
                labelScale = BaseLabelScale + (clamped - 1f),
            )
        }

        private const val BaseLabelScale = 0.96f
    }

    /**
     * This profile, or the phone-landscape ceiling if it would grow taller than that.
     *
     * Only a landscape phone is constrained — a tablet has the height to spare even sideways,
     * and portrait always honors the full slider on both.
     */
    fun constrainedForLandscape(isPhoneLandscape: Boolean): KeyboardSizingProfile =
        if (isPhoneLandscape && heightScale > PhoneLandscapeMaxScale) {
            scaled(PhoneLandscapeMaxScale)
        } else {
            this
        }
}
