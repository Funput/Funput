package app.funput.funput.ui.theme.custom.color

import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme

/** The sections the editor groups color roles into. */
internal enum class ThemeColorGroup(@StringRes val titleRes: Int) {
    Background(R.string.custom_theme_color_group_background),
    Keys(R.string.custom_theme_color_group_keys),
    Text(R.string.custom_theme_color_group_text),
    Advanced(R.string.custom_theme_color_group_advanced),
}

/**
 * Every color token a user can edit, paired with how to read and write it.
 *
 * Keeping the mapping in one table means a color control never names a specific token, so the
 * editor UI does not grow when the theme gains a color. Roles most people reach for come first;
 * the ones that only matter when chasing a detail sit under [ThemeColorGroup.Advanced].
 */
internal enum class ThemeColorRole(
    val group: ThemeColorGroup,
    @StringRes val labelRes: Int,
    /**
     * The role this one takes its colour from until somebody sets it apart.
     *
     * [KeyboardTheme] already declares these relationships as constructor defaults, and
     * `withAccent` maintains the accent ones. The editor used to throw that away and present every
     * token as an independent decision, which is how six real choices looked like twenty.
     */
    val follows: ThemeColorRole? = null,
) {
    BackgroundStart(ThemeColorGroup.Background, R.string.custom_theme_color_background_start),
    BackgroundEnd(ThemeColorGroup.Background, R.string.custom_theme_color_background_end),
    Key(ThemeColorGroup.Keys, R.string.custom_theme_color_key),
    SpecialKey(ThemeColorGroup.Keys, R.string.custom_theme_color_special_key),
    KeyBorder(ThemeColorGroup.Keys, R.string.custom_theme_color_key_border),
    PressedKey(ThemeColorGroup.Keys, R.string.custom_theme_color_pressed_key),
    Label(ThemeColorGroup.Text, R.string.custom_theme_color_label),
    SecondaryLabel(ThemeColorGroup.Text, R.string.custom_theme_color_secondary_label),
    Accent(ThemeColorGroup.Text, R.string.custom_theme_color_accent),
    AccentKey(ThemeColorGroup.Keys, R.string.custom_theme_color_accent_key, follows = Accent),
    SpecialLabel(ThemeColorGroup.Text, R.string.custom_theme_color_special_label, follows = Label),
    AccentLabel(ThemeColorGroup.Text, R.string.custom_theme_color_accent_label),
    SuggestionHighlight(
        ThemeColorGroup.Text,
        R.string.custom_theme_color_suggestion_highlight,
        follows = Accent,
    ),
    ActivatedKey(ThemeColorGroup.Advanced, R.string.custom_theme_color_activated_key),
    PressedKeyBorder(ThemeColorGroup.Advanced, R.string.custom_theme_color_pressed_key_border),
    ActivatedKeyBorder(ThemeColorGroup.Advanced, R.string.custom_theme_color_activated_key_border),
    PopupSurface(ThemeColorGroup.Advanced, R.string.custom_theme_color_popup_surface, follows = Key),
    KeyShadow(ThemeColorGroup.Advanced, R.string.custom_theme_color_key_shadow),
    SuggestionDivider(ThemeColorGroup.Advanced, R.string.custom_theme_color_suggestion_divider),
    PopupShadow(ThemeColorGroup.Advanced, R.string.custom_theme_color_popup_shadow);

    fun read(theme: KeyboardTheme): Int = when (this) {
        BackgroundStart -> theme.backgroundStartColor
        BackgroundEnd -> theme.backgroundEndColor
        Key -> theme.keyColor
        SpecialKey -> theme.specialKeyColor
        KeyBorder -> theme.keyBorderColor
        PressedKey -> theme.pressedKeyColor
        AccentKey -> theme.accentKeyColor
        Label -> theme.labelColor
        SpecialLabel -> theme.specialLabelColor
        SecondaryLabel -> theme.secondaryLabelColor
        Accent -> theme.accentColor
        AccentLabel -> theme.accentLabelColor
        SuggestionHighlight -> theme.suggestionHighlightColor
        ActivatedKey -> theme.activatedKeyColor
        PressedKeyBorder -> theme.pressedKeyBorderColor
        ActivatedKeyBorder -> theme.activatedKeyBorderColor
        PopupSurface -> theme.popupSurfaceColor
        KeyShadow -> theme.keyShadowColor
        SuggestionDivider -> theme.suggestionDividerColor
        PopupShadow -> theme.popupShadowColor
    }

    fun write(theme: KeyboardTheme, color: Int): KeyboardTheme = when (this) {
        BackgroundStart -> theme.copy(backgroundStartColor = color)
        BackgroundEnd -> theme.copy(backgroundEndColor = color)
        Key -> theme.copy(keyColor = color)
        SpecialKey -> theme.copy(specialKeyColor = color)
        KeyBorder -> theme.copy(keyBorderColor = color)
        PressedKey -> theme.copy(pressedKeyColor = color)
        AccentKey -> theme.copy(accentKeyColor = color)
        Label -> theme.copy(labelColor = color)
        SpecialLabel -> theme.copy(specialLabelColor = color)
        SecondaryLabel -> theme.copy(secondaryLabelColor = color)
        Accent -> theme.copy(accentColor = color)
        AccentLabel -> theme.copy(accentLabelColor = color)
        SuggestionHighlight -> theme.copy(suggestionHighlightColor = color)
        ActivatedKey -> theme.copy(activatedKeyColor = color)
        PressedKeyBorder -> theme.copy(pressedKeyBorderColor = color)
        ActivatedKeyBorder -> theme.copy(activatedKeyBorderColor = color)
        PopupSurface -> theme.copy(popupSurfaceColor = color)
        KeyShadow -> theme.copy(keyShadowColor = color)
        SuggestionDivider -> theme.copy(suggestionDividerColor = color)
        PopupShadow -> theme.copy(popupShadowColor = color)
    }
}
