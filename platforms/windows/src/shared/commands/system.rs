//! The two commands with an OS side effect outside Funput's own files: the
//! autostart registry entry, and handing a URL to the system browser.

use crate::shared::shell;

/// Persist the launch-at-login preference and mirror it into the OS autostart
/// (HKCU `…\Run`) via `auto-launch`.
pub fn set_launch_at_login(on: bool) {
    shell::set_launch_at_login(on);
    sync_autostart(on);
}

/// Bring the OS autostart entry in line with `on`. Called on startup (from the
/// persisted preference) and whenever the toggle changes.
pub fn sync_autostart(on: bool) {
    let Some(auto) = autolaunch() else { return };
    let _ = if on { auto.enable() } else { auto.disable() };
}

fn autolaunch() -> Option<auto_launch::AutoLaunch> {
    let exe = std::env::current_exe().ok()?;
    auto_launch::AutoLaunchBuilder::new()
        .set_app_name("Funput")
        .set_app_path(&exe.to_string_lossy())
        .build()
        .ok()
}

/// Open an external link (GitHub / Website) in the system browser.
pub fn open_url(url: &str) {
    let _ = open::that(url);
}
