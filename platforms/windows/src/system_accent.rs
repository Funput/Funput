//! Windows accent color for the Slint `Theme` global.
//!
//! Slint's `Palette.accent-*` follows the widget *style*, not the OS accent
//! (except the Qt style). On Windows we read the DWM accent from the registry
//! and push it into `Theme`; if that fails we fall back to orange.

use slint::Color;

use crate::Theme;

/// Orange-600 — used when the system accent cannot be read.
const FALLBACK: Color = Color::from_rgb_u8(0xea, 0x58, 0x0c);
const FALLBACK_TEXT: Color = Color::from_rgb_u8(0xff, 0xff, 0xff);
const DARK_TEXT: Color = Color::from_rgb_u8(0x1c, 0x19, 0x17);

/// Apply the resolved accent (system or orange fallback) to a Theme global.
pub fn apply(theme: &Theme) {
    let (fill, text) = match read_dwm_accent().or_else(read_highlight) {
        Some((r, g, b)) => {
            let fill = Color::from_rgb_u8(r, g, b);
            (fill, contrasting_text(r, g, b))
        }
        None => (FALLBACK, FALLBACK_TEXT),
    };
    theme.set_accent(fill);
    theme.set_accent_text(text);
}

/// `HKCU\Software\Microsoft\Windows\DWM\AccentColor` (COLORREF / ABGR).
fn read_dwm_accent() -> Option<(u8, u8, u8)> {
    #[cfg(windows)]
    {
        use windows::core::PCWSTR;
        use windows::Win32::Foundation::ERROR_SUCCESS;
        use windows::Win32::System::Registry::{
            RegCloseKey, RegOpenKeyExW, RegQueryValueExW, HKEY_CURRENT_USER, KEY_READ, REG_DWORD,
            REG_VALUE_TYPE,
        };

        let subkey: Vec<u16> = "Software\\Microsoft\\Windows\\DWM\0"
            .encode_utf16()
            .collect();
        let value_name: Vec<u16> = "AccentColor\0".encode_utf16().collect();

        unsafe {
            let mut key = Default::default();
            if RegOpenKeyExW(
                HKEY_CURRENT_USER,
                PCWSTR(subkey.as_ptr()),
                None,
                KEY_READ,
                &mut key,
            ) != ERROR_SUCCESS
            {
                return None;
            }
            let mut data = [0u8; 4];
            let mut data_size = data.len() as u32;
            let mut ty = REG_VALUE_TYPE::default();
            let status = RegQueryValueExW(
                key,
                PCWSTR(value_name.as_ptr()),
                None,
                Some(&mut ty),
                Some(data.as_mut_ptr()),
                Some(&mut data_size),
            );
            let _ = RegCloseKey(key);
            if status != ERROR_SUCCESS || ty != REG_DWORD || data_size < 3 {
                return None;
            }
            // COLORREF layout: 0x00BBGGRR
            Some((data[0], data[1], data[2]))
        }
    }
    #[cfg(not(windows))]
    {
        None
    }
}

/// Older fallback: `COLOR_HIGHLIGHT` from GetSysColor.
fn read_highlight() -> Option<(u8, u8, u8)> {
    #[cfg(windows)]
    {
        use windows::Win32::Graphics::Gdi::{GetSysColor, COLOR_HIGHLIGHT};

        let color = unsafe { GetSysColor(COLOR_HIGHLIGHT) };
        let r = (color & 0xff) as u8;
        let g = ((color >> 8) & 0xff) as u8;
        let b = ((color >> 16) & 0xff) as u8;
        // Skip near-black / near-white which are rarely intentional accents.
        if (r as u16 + g as u16 + b as u16) < 40 || (r as u16 + g as u16 + b as u16) > 720 {
            return None;
        }
        Some((r, g, b))
    }
    #[cfg(not(windows))]
    {
        None
    }
}

fn contrasting_text(r: u8, g: u8, b: u8) -> Color {
    // Relative luminance (sRGB, simplified).
    let lum = 0.2126 * srgb(r) + 0.7152 * srgb(g) + 0.0722 * srgb(b);
    if lum > 0.55 {
        DARK_TEXT
    } else {
        FALLBACK_TEXT
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
