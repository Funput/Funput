package app.funput.funput.ime.settings

internal object ToneStyleSettingCodec {
    fun encode(style: ToneStyle): String = when (style) {
        ToneStyle.TRADITIONAL -> TraditionalValue
        ToneStyle.MODERN -> ModernValue
    }

    fun decode(
        value: String?,
        default: ToneStyle = ToneStyleSettings.FallbackToneStyle,
    ): ToneStyle = when (value) {
        ModernValue -> ToneStyle.MODERN
        TraditionalValue -> ToneStyle.TRADITIONAL
        else -> default
    }

    private const val TraditionalValue = "traditional"
    private const val ModernValue = "modern"
}
