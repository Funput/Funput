//! Windows 11 system backdrop (Mica / Acrylic) for the Slint windows.
//!
//! Slint exposes no backdrop API, so we reach the underlying `winit::Window`
//! through `i-slint-backend-winit`. That crate is internal to Slint and carries no
//! semver guarantee — hence the `=1.17.0` pin in Cargo.toml, which must be bumped
//! in lockstep with `slint`.
//!
//! Two steps are needed and they happen at different times. DWM only composites a
//! backdrop where the window leaves pixels transparent, so the window has to be
//! *created* transparent (a backend hook, before any window exists); the material
//! itself is then set on the live window, after it has been shown.

use std::sync::OnceLock;

use i_slint_backend_winit::winit::platform::windows::{
    BackdropType, CornerPreference, WindowExtWindows,
};
use i_slint_backend_winit::winit::window::Window as WinitWindow;
use i_slint_backend_winit::WinitWindowAccessor;

/// `DWMWA_SYSTEMBACKDROP_TYPE` is documented from Windows 11 build 22621.
const MIN_BUILD: u32 = 22621;

/// Which system material DWM should composite behind a window.
pub enum Material {
    /// Mica — long-lived windows that own their place (Settings, Onboarding).
    Window,
    /// Acrylic — transient surfaces layered over other content (flyouts, overlays).
    Transient,
}

/// Whether this machine can draw a system backdrop at all.
pub fn supported() -> bool {
    static SUPPORTED: OnceLock<bool> = OnceLock::new();
    *SUPPORTED.get_or_init(|| build_number().is_some_and(|build| build >= MIN_BUILD))
}

/// Install a winit backend that creates transparent windows.
///
/// Must run before the first window is constructed, and only where a backdrop is
/// actually available: a transparent window with nothing behind it renders as a
/// hole on Windows 10.
pub fn install_backend() {
    if !supported() {
        return;
    }
    let Ok(backend) = i_slint_backend_winit::Backend::builder()
        .with_window_attributes_hook(|attributes| attributes.with_transparent(true))
        .build()
    else {
        return;
    };
    let _ = slint::platform::set_platform(Box::new(backend));
}

/// Ask DWM to composite `material` behind a window that has already been shown.
pub fn apply(window: &slint::Window, material: Material) {
    if !supported() {
        return;
    }
    let backdrop = match material {
        Material::Window => BackdropType::MainWindow,
        Material::Transient => BackdropType::TransientWindow,
    };
    window.with_winit_window(|winit: &WinitWindow| {
        winit.set_system_backdrop(backdrop);
        winit.set_corner_preference(CornerPreference::Round);
    });
}

/// `HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\CurrentBuildNumber` (REG_SZ).
///
/// The Win32 version APIs shim their answer for unmanifested processes, so the
/// registry is the honest source for the build number.
fn build_number() -> Option<u32> {
    use windows::core::PCWSTR;
    use windows::Win32::Foundation::ERROR_SUCCESS;
    use windows::Win32::System::Registry::{
        RegCloseKey, RegOpenKeyExW, RegQueryValueExW, HKEY_LOCAL_MACHINE, KEY_READ, REG_SZ,
        REG_VALUE_TYPE,
    };

    let subkey: Vec<u16> = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\0"
        .encode_utf16()
        .collect();
    let value_name: Vec<u16> = "CurrentBuildNumber\0".encode_utf16().collect();

    unsafe {
        let mut key = Default::default();
        if RegOpenKeyExW(
            HKEY_LOCAL_MACHINE,
            PCWSTR(subkey.as_ptr()),
            None,
            KEY_READ,
            &mut key,
        ) != ERROR_SUCCESS
        {
            return None;
        }
        let mut data = [0u8; 32];
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
        if status != ERROR_SUCCESS || ty != REG_SZ {
            return None;
        }
        let text: Vec<u16> = data[..data_size as usize]
            .chunks_exact(2)
            .map(|pair| u16::from_le_bytes([pair[0], pair[1]]))
            .take_while(|unit| *unit != 0)
            .collect();
        String::from_utf16(&text).ok()?.trim().parse().ok()
    }
}
