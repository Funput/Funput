package app.funput.funput.ui.theme.custom

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

internal fun KeyboardThemeDescriptor?.initialThemeName(): String = this?.name.orEmpty()

internal fun KeyboardThemeDescriptor?.initialBaseThemeValue(): String =
    this?.baseThemeId?.value ?: KeyboardThemeId.Default.value

internal fun KeyboardThemeDescriptor?.initialBackgroundImageSource(): String? =
    this?.backgroundImage?.source

internal fun KeyboardThemeDescriptor?.initialBackgroundImageOpacity(): Float =
    this?.backgroundImage?.opacity ?: DefaultBackgroundImageOpacity
