package app.funput.funput.ui.theme

import app.funput.funput.theme.KeyboardThemeId

/**
 * Ties the keyboard preview on a gallery card to the one at the top of the theme studio, so
 * opening a theme grows the card's keyboard into the editor's instead of cutting to it.
 */
internal fun themePreviewSharedKey(themeId: KeyboardThemeId?): Any? =
    themeId?.let { id -> "theme-preview-${id.value}" }
