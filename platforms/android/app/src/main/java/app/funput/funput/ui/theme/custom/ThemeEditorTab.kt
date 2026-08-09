package app.funput.funput.ui.theme.custom

import androidx.annotation.StringRes
import app.funput.funput.R

/**
 * The editor's four pages.
 *
 * One scrolling page held every control the theme has, and the length of it was the complaint. The
 * same controls split by what you are thinking about — the theme as a whole, its background, its
 * keys, what happens under a finger — put roughly a screenful in each, which is how the iOS editor
 * is organised and why it reads as smaller while holding as much.
 */
internal enum class ThemeEditorTab(@param:StringRes val titleRes: Int) {
    General(R.string.custom_theme_tab_general),
    Background(R.string.custom_theme_tab_background),
    Keys(R.string.custom_theme_tab_keys),
    Pressed(R.string.custom_theme_tab_pressed),
}
