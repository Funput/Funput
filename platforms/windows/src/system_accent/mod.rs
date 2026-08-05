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
const LIGHT_TEXT: Color = Color::from_rgb_u8(0xff, 0xff, 0xff);
const DARK_TEXT: Color = Color::from_rgb_u8(0x1c, 0x19, 0x17);

/// Apply the resolved accent and its readable text colour to a Theme global.
pub fn apply(theme: &Theme) {
    let (r, g, b) = resolve().unwrap_or_else(|| style_accent(theme));
    theme.set_accent(Color::from_rgb_u8(r, g, b));
    theme.set_accent_text(contrasting_text(r, g, b));
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

fn contrasting_text(r: u8, g: u8, b: u8) -> Color {
    // Relative luminance (sRGB).
    let lum = 0.2126 * srgb(r) + 0.7152 * srgb(g) + 0.0722 * srgb(b);
    // Break-even point where white and DARK_TEXT score the same WCAG ratio
    // against this fill: (L + 0.05)² = 1.05 × (L_dark + 0.05). Anything brighter
    // reads better with dark text — mid-luminance accents (orange, teal, lime)
    // land above it, where white would only reach ~3.5:1.
    if lum > 0.2 {
        DARK_TEXT
    } else {
        LIGHT_TEXT
    }
}

fn srgb(c: u8) -> f32 {
    let c = c as f32 / 255.0;
    if c <= 0.03928 {
        c / 12.92
    } else {
        ((c + 0.055) / 1.055).powf(2.4)
    }
}
