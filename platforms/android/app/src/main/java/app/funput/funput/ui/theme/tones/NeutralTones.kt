package app.funput.funput.ui.theme.tones

import androidx.compose.ui.graphics.Color

/**
 * Neutral and error tonal palettes. Both neutrals take the brand orange hue at the low chroma
 * Material 3 uses for surfaces (6 for neutral, 12 for neutral variant), which is what keeps every
 * Funput surface faintly warm instead of grey. Error keeps the Material baseline red.
 *
 * See [PrimaryTones] for how these were generated.
 */
internal object NeutralTones {
    val T0 = Color(0xFF000000)
    val T4 = Color(0xFF130D08)
    val T6 = Color(0xFF19120C)
    val T10 = Color(0xFF221A14)
    val T12 = Color(0xFF261E18)
    val T17 = Color(0xFF302822)
    val T20 = Color(0xFF382F28)
    val T22 = Color(0xFF3C332C)
    val T24 = Color(0xFF413731)
    val T87 = Color(0xFFE6D7CD)
    val T90 = Color(0xFFEFE0D6)
    val T92 = Color(0xFFF5E5DB)
    val T94 = Color(0xFFFBEBE1)
    val T95 = Color(0xFFFEEEE4)
    val T96 = Color(0xFFFFF1E7)
    val T98 = Color(0xFFFFF8F3)
    val T100 = Color(0xFFFFFFFF)
}

internal object NeutralVariantTones {
    val T30 = Color(0xFF564334)
    val T50 = Color(0xFF897362)
    val T60 = Color(0xFFA48C7A)
    val T80 = Color(0xFFDDC2AE)
    val T90 = Color(0xFFF9DDC9)
}

internal object ErrorTones {
    val T10 = Color(0xFF410001)
    val T20 = Color(0xFF680003)
    val T30 = Color(0xFF930006)
    val T40 = Color(0xFFBA1B1B)
    val T80 = Color(0xFFFFB4A9)
    val T90 = Color(0xFFFFDAD4)
    val T100 = Color(0xFFFFFFFF)
}
