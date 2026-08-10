package app.funput.funput.ui.theme

import androidx.compose.material3.lightColorScheme
import app.funput.funput.ui.theme.tones.ErrorTones
import app.funput.funput.ui.theme.tones.NeutralTones
import app.funput.funput.ui.theme.tones.NeutralVariantTones
import app.funput.funput.ui.theme.tones.PrimaryTones
import app.funput.funput.ui.theme.tones.SecondaryTones
import app.funput.funput.ui.theme.tones.TertiaryTones

/**
 * The brand light scheme, used whenever dynamic colour is unavailable or turned off.
 *
 * Every role is assigned. That matters: any role left out falls back to Material's baseline
 * purple, which is how stray violet used to reach dialogs, snackbars and elevation tints.
 */
internal val FunputLightColors = lightColorScheme(
    primary = PrimaryTones.T44,
    onPrimary = PrimaryTones.T100,
    primaryContainer = PrimaryTones.T90,
    onPrimaryContainer = PrimaryTones.T10,
    inversePrimary = PrimaryTones.T80,
    secondary = SecondaryTones.T40,
    onSecondary = SecondaryTones.T100,
    secondaryContainer = SecondaryTones.T90,
    onSecondaryContainer = SecondaryTones.T10,
    tertiary = TertiaryTones.T40,
    onTertiary = TertiaryTones.T100,
    tertiaryContainer = TertiaryTones.T90,
    onTertiaryContainer = TertiaryTones.T10,
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
    error = ErrorTones.T40,
    onError = ErrorTones.T100,
    errorContainer = ErrorTones.T90,
    onErrorContainer = ErrorTones.T10,
    background = NeutralTones.T98,
    onBackground = NeutralTones.T10,
    surface = NeutralTones.T98,
    onSurface = NeutralTones.T10,
    surfaceDim = NeutralTones.T87,
    surfaceBright = NeutralTones.T98,
    surfaceContainerLowest = NeutralTones.T100,
    surfaceContainerLow = NeutralTones.T96,
    surfaceContainer = NeutralTones.T94,
    surfaceContainerHigh = NeutralTones.T92,
    surfaceContainerHighest = NeutralTones.T90,
    surfaceVariant = NeutralVariantTones.T90,
    onSurfaceVariant = NeutralVariantTones.T30,
    surfaceTint = PrimaryTones.T44,
    inverseSurface = NeutralTones.T20,
    inverseOnSurface = NeutralTones.T95,
    outline = NeutralVariantTones.T50,
    outlineVariant = NeutralVariantTones.T80,
    scrim = NeutralTones.T0,
)
