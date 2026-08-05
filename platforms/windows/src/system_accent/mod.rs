//! Windows accent color for the Slint `Theme` global.
//!
//! Slint does track an OS accent (`Palette.accent-background`), but only through
//! `DwmGetColorizationColor` — the blended title-bar colour — and it re-lightens
//! the result to the Fluent palette's own lightness. The registry sources below
//! give the accent the rest of the shell actually paints, so they win; Slint's
//! value is the fallback. Read once per window: these are short-lived processes.

mod registry;

use slint::Color;

use crate::Theme;

/// Orange-600 — brand colour, only reached off-Windows or if Slint has no accent.
const FALLBACK: (u8, u8, u8) = (0xea, 0x58, 0x0c);

/// Apply the resolved accent to a Theme global. The theme re-lightens it per
/// colour scheme, so only the raw hue matters here.
pub fn apply(theme: &Theme) {
    let (r, g, b) = resolve().unwrap_or_else(|| style_accent(theme));
    theme.set_accent_source(Color::from_rgb_u8(r, g, b));
}

/// Registry sources in descending order of fidelity. `AccentPalette` is what
/// WinUI itself reads, so trying it first keeps Funput in step with the shell
/// even when the DWM value lags behind a colour change.
fn resolve() -> Option<(u8, u8, u8)> {
    registry::accent_palette()
        .or_else(|| registry::dword_rgb(registry::DWM, "AccentColor"))
        .or_else(|| registry::dword_rgb(registry::EXPLORER_ACCENT, "AccentColorMenu"))
}

/// Last resort: Slint's own accent (`DwmGetColorizationColor`, then
/// COLOR_HIGHLIGHT, re-lightened by the Fluent palette). Still preferable to the
/// brand orange, which has nothing to do with the user's system.
fn style_accent(theme: &Theme) -> (u8, u8, u8) {
    let color = theme.get_style_accent().color();
    if color.alpha() == 0 {
        return FALLBACK;
    }
    (color.red(), color.green(), color.blue())
}