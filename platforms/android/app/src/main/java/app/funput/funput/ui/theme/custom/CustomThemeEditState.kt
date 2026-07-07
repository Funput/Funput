package app.funput.funput.ui.theme.custom

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

internal fun KeyboardThemeDescriptor?.initialThemeName(): String = this?.name.orEmpty()

internal fun KeyboardThemeDescriptor?.initialBaseThemeValue(): String =
    this?.baseThemeId?.value ?: KeyboardThemeId.Default.value

internal fun KeyboardThemeDescriptor?.initialAccentColor(): Int =
    this?.theme?.accentColor ?: AccentPresets.first().argb

internal fun KeyboardThemeDescriptor?.initialBackgroundImageSource(): String? =
    this?.backgroundImage?.source

internal fun KeyboardThemeDescriptor?.initialBackgroundImageOpacity(): Float =
    this?.backgroundImage?.opacity ?: DefaultBackgroundImageOpacity

internal fun KeyboardThemeDescriptor?.initialKeyBackgroundOpacity(): Float =
    this?.theme?.keyColor?.alphaFraction()
        ?.coerceIn(MinKeyBackgroundOpacity, MaxKeyBackgroundOpacity)
        ?: DefaultKeyBackgroundOpacity

private fun Int.alphaFraction(): Float = (this ushr AlphaShift) / MaxAlpha

private const val AlphaShift = 24
private const val MaxAlpha = 255f
