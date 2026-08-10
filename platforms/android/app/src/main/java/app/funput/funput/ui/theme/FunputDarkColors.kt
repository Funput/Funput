package app.funput.funput.ui.theme

import androidx.compose.material3.darkColorScheme
import app.funput.funput.ui.theme.tones.ErrorTones
import app.funput.funput.ui.theme.tones.NeutralTones
import app.funput.funput.ui.theme.tones.NeutralVariantTones
import app.funput.funput.ui.theme.tones.PrimaryTones
import app.funput.funput.ui.theme.tones.SecondaryTones
import app.funput.funput.ui.theme.tones.TertiaryTones

/** The brand dark scheme. Mirrors [FunputLightColors]; see there for why every role is assigned. */
internal val FunputDarkColors = darkColorScheme(
    primary = PrimaryTones.T80,
    onPrimary = PrimaryTones.T20,
    primaryContainer = PrimaryTones.T30,
    onPrimaryContainer = PrimaryTones.T90,
    inversePrimary = PrimaryTones.T40,
    secondary = SecondaryTones.T80,
    onSecondary = SecondaryTones.T20,
    secondaryContainer = SecondaryTones.T30,
    onSecondaryContainer = SecondaryTones.T90,
    tertiary = TertiaryTones.T80,
    onTertiary = TertiaryTones.T20,
    tertiaryContainer = TertiaryTones.T30,
    onTertiaryContainer = TertiaryTones.T90,
    primaryFixed = PrimaryTones.T90,
    primaryFixedDim = PrimaryTones.T80,
    onPrimaryFixed = PrimaryTones.T10,
    onPrimaryFixedVariant = PrimaryTones.T30,
    secondaryFixed = SecondaryTones.T90,
    secondaryFixedDim = SecondaryTones.T80,
    onSecondaryFixed = SecondaryTones.T10,
    onSecondaryFixedVariant = SecondaryTones.T30,
    tertiaryFixed = TertiaryTones.T90,
    tertiaryFixedDim = TertiaryTones.T80,
    onTertiaryFixed = TertiaryTones.T10,
    onTertiaryFixedVariant = TertiaryTones.T30,
    error = ErrorTones.T80,
    onError = ErrorTones.T20,
    errorContainer = ErrorTones.T30,
    onErrorContainer = ErrorTones.T90,
    // Near black rather than dark grey. Amber has no chroma to spare at this end of the scale,
    // so what makes it glow is the drop in luminance underneath it — the ground, not the accent.
    background = NeutralTones.T3,
    onBackground = NeutralTones.T92,
    surface = NeutralTones.T3,
    onSurface = NeutralTones.T92,
    surfaceDim = NeutralTones.T3,
    surfaceBright = NeutralTones.T24,
    surfaceContainerLowest = NeutralTones.T0,
    surfaceContainerLow = NeutralTones.T6,
    surfaceContainer = NeutralTones.T8,
    surfaceContainerHigh = NeutralTones.T14,
    surfaceContainerHighest = NeutralTones.T20,
    surfaceVariant = NeutralVariantTones.T30,
    onSurfaceVariant = NeutralVariantTones.T80,
    surfaceTint = PrimaryTones.T80,
    inverseSurface = NeutralTones.T90,
    inverseOnSurface = NeutralTones.T20,
    outline = NeutralVariantTones.T60,
    outlineVariant = NeutralVariantTones.T30,
    scrim = NeutralTones.T0,
)
