//! Whether this process is running as an MSIX / Store package.
//!
//! A packaged install lives under `WindowsApps` (read-only). The portable
//! `.exe` must keep writing next to itself, swapping updates, and using
//! `HKCU\…\Run`; those all fail or violate Store policy when packaged.

/// About-pane copy when the Store owns updates. Vietnamese: the Settings UI is.
pub const STORE_UPDATE_MESSAGE: &str = "Cập nhật qua Microsoft Store.";

/// `GetCurrentPackageFullName` when this process has no package identity.
const APPMODEL_ERROR_NO_PACKAGE: u32 = 15700;
/// First call (null buffer) when a package identity exists.
const ERROR_INSUFFICIENT_BUFFER: u32 = 122;
const ERROR_SUCCESS: u32 = 0;

/// True when Windows reports a package identity for this process.
pub fn is_packaged() -> bool {
    packaged_from_status(probe_status())
}

/// Interprets the status of `GetCurrentPackageFullName`'s length probe.
///
/// Packaged: `ERROR_SUCCESS` or `ERROR_INSUFFICIENT_BUFFER`. Unpackaged:
/// `APPMODEL_ERROR_NO_PACKAGE`. Any other code is treated as unpackaged so a
/// surprising failure does not disable portable update/autostart.
pub fn packaged_from_status(status: u32) -> bool {
    matches!(status, ERROR_SUCCESS | ERROR_INSUFFICIENT_BUFFER)
}

#[cfg(windows)]
fn probe_status() -> u32 {
    use windows::Win32::Storage::Packaging::Appx::GetCurrentPackageFullName;

    let mut len = 0u32;
    unsafe { GetCurrentPackageFullName(&raw mut len, None) }.0
}

#[cfg(not(windows))]
fn probe_status() -> u32 {
    APPMODEL_ERROR_NO_PACKAGE
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn insufficient_buffer_means_packaged() {
        assert!(packaged_from_status(ERROR_INSUFFICIENT_BUFFER));
    }

    #[test]
    fn success_means_packaged() {
        assert!(packaged_from_status(ERROR_SUCCESS));
    }

    #[test]
    fn no_package_means_portable() {
        assert!(!packaged_from_status(APPMODEL_ERROR_NO_PACKAGE));
    }

    #[test]
    fn unknown_status_stays_portable() {
        assert!(!packaged_from_status(5));
        assert!(!packaged_from_status(u32::MAX));
    }

    #[test]
    fn store_message_is_vietnamese() {
        assert!(STORE_UPDATE_MESSAGE.contains("Microsoft Store"));
    }
}
