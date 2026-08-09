package app.funput.funput.ui.theme.custom

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

/**
 * A new theme starts named rather than blank.
 *
 * An empty name used to disable saving, which made naming the thing the first demand the editor
 * made and the least interesting decision in it. A default that can be typed over asks nothing.
 */
internal fun KeyboardThemeDescriptor?.initialThemeName(): String =
    this?.name ?: DefaultCustomThemeName

internal const val DefaultCustomThemeName = "Theme của tôi"

internal fun KeyboardThemeDescriptor?.initialBaseThemeValue(): String =
    this?.baseThemeId?.value ?: KeyboardThemeId.Default.value
