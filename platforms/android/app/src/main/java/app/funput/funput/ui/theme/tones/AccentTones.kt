package app.funput.funput.ui.theme.tones

import androidx.compose.ui.graphics.Color

/**
 * Accent tonal palettes for the brand colour scheme, generated the way Material Theme Builder
 * generates them: take the seed's HCT hue, pin a chroma, and read off the tones the Material 3
 * roles ask for. Only the tones [app.funput.funput.ui.theme.FunputLightColors] and
 * [app.funput.funput.ui.theme.FunputDarkColors] actually reference are kept.
 *
 * Seeds are the brand colours in [app.funput.funput.ui.theme.BrandOrange],
 * [app.funput.funput.ui.theme.BrandPink] and [app.funput.funput.ui.theme.BrandBlue], so the
 * fallback scheme still reads as Funput on devices without dynamic colour.
 *
 * To regenerate: Material Theme Builder, "core colours" mode, primary #EF8A1A (chroma 48),
 * secondary #FF4F78 (chroma 26), tertiary #1F78FF (chroma 32).
 */
internal object PrimaryTones {
    val T10 = Color(0xFF2F1500)
    val T20 = Color(0xFF4D2700)
    val T30 = Color(0xFF6D3900)
    val T40 = Color(0xFF8F4D00)
    /**
     * Light mode's primary, one step lighter than the tone 40 Material would pick. Orange at
     * tone 40 is brown — the sRGB gamut caps chroma there, so no amount of saturation reaches
     * the brand's colour. Tone 44 is the lightest step that still clears 4.5:1 against every
     * surface it can land on, down to surfaceContainerHigh at 4.52:1. FunputContrastTest holds
     * that line; tone 46 was tried first and failed it.
     */
    val T44 = Color(0xFF9E5600)
    val T80 = Color(0xFFFFB776)
    val T90 = Color(0xFFFFDCBF)
    val T100 = Color(0xFFFFFFFF)
}

internal object SecondaryTones {
    val T10 = Color(0xFF330F16)
    val T20 = Color(0xFF4D232A)
    val T30 = Color(0xFF673940)
    val T40 = Color(0xFF825057)
    val T80 = Color(0xFFF7B6BE)
    val T90 = Color(0xFFFFDADF)
    val T100 = Color(0xFFFFFFFF)
}

internal object TertiaryTones {
    val T10 = Color(0xFF001943)
    val T20 = Color(0xFF1A2F59)
    val T30 = Color(0xFF324671)
    val T40 = Color(0xFF4A5E8A)
    val T80 = Color(0xFFB2C6F9)
    val T90 = Color(0xFFD8E2FF)
    val T100 = Color(0xFFFFFFFF)
}
