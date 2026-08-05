//! Registry / GDI readers for the Windows accent color.
//!
//! Windows publishes the accent in several places and they do not always agree.
//! Each reader below returns plain `(r, g, b)` so the caller can try them in
//! preference order.

/// `HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Accent`
pub(super) const EXPLORER_ACCENT: &str =
    "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Accent";
/// `HKCU\Software\Microsoft\Windows\DWM`
pub(super) const DWM: &str = "Software\\Microsoft\\Windows\\DWM";

/// Read a value from HKCU into `buf`. Returns the value type and byte count.
#[cfg(windows)]
fn query(subkey: &str, value: &str, buf: &mut [u8]) -> Option<(u32, usize)> {
    use windows::core::PCWSTR;
    use windows::Win32::Foundation::ERROR_SUCCESS;
    use windows::Win32::System::Registry::{
        RegCloseKey, RegOpenKeyExW, RegQueryValueExW, HKEY_CURRENT_USER, KEY_READ, REG_VALUE_TYPE,
    };

    let subkey: Vec<u16> = subkey.encode_utf16().chain(Some(0)).collect();
    let value: Vec<u16> = value.encode_utf16().chain(Some(0)).collect();
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
        let mut size = buf.len() as u32;
        let mut ty = REG_VALUE_TYPE::default();
        let status = RegQueryValueExW(
            key,
            PCWSTR(value.as_ptr()),
            None,
            Some(&mut ty),
            Some(buf.as_mut_ptr()),
            Some(&mut size),
        );
        let _ = RegCloseKey(key);
        if status != ERROR_SUCCESS {
            return None;
        }
        Some((ty.0, size as usize))
    }
}

/// `AccentPalette`: eight RGBA entries, darkest first. Entry 4 is the one
/// `UISettings.GetColorValue(UIColorType.Accent)` hands to WinUI apps, so it is
/// the closest match to what the rest of the shell paints.
pub(super) fn accent_palette() -> Option<(u8, u8, u8)> {
    #[cfg(windows)]
    {
        use windows::Win32::System::Registry::REG_BINARY;

        let mut buf = [0u8; 32];
        let (ty, size) = query(EXPLORER_ACCENT, "AccentPalette", &mut buf)?;
        if ty != REG_BINARY.0 || size < 20 {
            return None;
        }
        Some((buf[16], buf[17], buf[18]))
    }
    #[cfg(not(windows))]
    {
        None
    }
}

/// An ABGR `REG_DWORD` (COLORREF layout `0x00BBGGRR`, so the bytes read R, G, B).
pub(super) fn dword_rgb(subkey: &str, value: &str) -> Option<(u8, u8, u8)> {
    #[cfg(windows)]
    {
        use windows::Win32::System::Registry::REG_DWORD;

        let mut buf = [0u8; 4];
        let (ty, size) = query(subkey, value, &mut buf)?;
        if ty != REG_DWORD.0 || size < 3 {
            return None;
        }
        Some((buf[0], buf[1], buf[2]))
    }
    #[cfg(not(windows))]
    {
        let _ = (subkey, value);
        None
    }
}
