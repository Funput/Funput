//! The two commands with an OS side effect outside Funput's own files: the
//! autostart entry, and handing a URL to the system browser.

use crate::shared::packaged::{is_packaged, startup_task};
use crate::shared::shell;

/// Apply the launch-at-login toggle, persist what actually took effect, and
/// return it. The portable build writes HKCU `…\Run` via `auto-launch` and
/// always gets `on`. A Store build asks its startup task, which the user or an
/// organization can hold the other way — the caller then flips the switch back.
pub fn set_launch_at_login(on: bool) -> bool {
    let effective = if is_packaged() {
        // No answer from WinRT is not a refusal: keep the user's choice.
        startup_task::request(on).map_or(on, startup_task::State::is_enabled)
    } else {
        sync_run_key(on);
        on
    };
    shell::set_launch_at_login(effective);
    effective
}

/// Called on startup with the persisted preference.
///
/// Portable: rewrite the Run key to match it. Store: Windows owns the startup
/// task, and the user may have changed it in Settings since the last run, so
/// adopt its state into the preference rather than overwrite their choice.
pub fn sync_autostart(saved: bool) {
    if !is_packaged() {
        sync_run_key(saved);
        return;
    }
    if let Some(state) = startup_task::current()
        && state.is_enabled() != saved
    {
        shell::set_launch_at_login(state.is_enabled());
    }
}

fn sync_run_key(on: bool) {
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
