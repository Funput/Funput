//! Spawning and killing a UI child process.
//!
//! At most one exists at a time — opening Settings closes the flyout, and vice
//! versa — so a single slot holds it. That slot is a `thread_local` because only
//! the hook/tray thread ever touches it. The Control Center's own toggle and reap
//! rules live in [`super::flyout`].

use std::cell::RefCell;
use std::os::windows::io::AsRawHandle;
use std::process::{Child, Command};
use std::time::{SystemTime, UNIX_EPOCH};

use windows::Win32::Foundation::HANDLE;

thread_local! {
    pub(super) static UI_PROCESS: RefCell<Option<(UiKind, Child)>> = const { RefCell::new(None) };
}

#[derive(Clone, Copy, PartialEq, Eq)]
pub(super) enum UiKind {
    Settings,
    Onboarding,
    ControlCenter,
}

pub(super) const PARENT_PID_ENV: &str = "FUNPUT_PARENT_PID";
const SETTINGS_ACTIVE_ENV: &str = "FUNPUT_SETTINGS_ACTIVE";

pub(crate) fn launch_settings(check_update: bool) {
    launch_settings_tab(
        if check_update { "about" } else { "overview" },
        check_update,
    );
}

pub(super) fn launch_settings_tab(tab: &str, check_update: bool) {
    terminate_children();
    let arg = if check_update {
        "--settings-check-update"
    } else {
        "--settings"
    };
    spawn_child(arg, UiKind::Settings, &[(SETTINGS_ACTIVE_ENV, tab)]);
}

pub(crate) fn launch_onboarding() {
    terminate_children();
    spawn_child("--onboarding", UiKind::Onboarding, &[]);
}

pub(super) fn spawn_child(arg: &str, kind: UiKind, extra_env: &[(&str, &str)]) {
    let Some(exe) = std::env::current_exe().ok() else {
        return;
    };
    let mut cmd = Command::new(exe);
    cmd.arg(arg)
        .env(PARENT_PID_ENV, std::process::id().to_string());
    for (key, value) in extra_env {
        cmd.env(*key, *value);
    }
    if let Some(child) = cmd.spawn().ok() {
        UI_PROCESS.with(|cell| *cell.borrow_mut() = Some((kind, child)));
    }
}

/// Raw handle of the tracked UI child, for the hook thread's wait set.
///
/// Borrowed, never owned: the `Child` in [`UI_PROCESS`] stays the only owner, so
/// the caller must not close it. Only valid until that slot is cleared, which
/// this thread alone does — see [`crate::background::instance::wait_activate_message_or_child`].
pub(crate) fn current_child_handle() -> Option<HANDLE> {
    UI_PROCESS.with(|cell| {
        cell.borrow()
            .as_ref()
            .map(|(_, child)| HANDLE(child.as_raw_handle()))
    })
}

pub(crate) fn terminate_children() {
    UI_PROCESS.with(|cell| {
        if let Some((_, mut child)) = cell.borrow_mut().take() {
            let _ = child.kill();
            let _ = child.wait();
        }
    });
}

pub(super) fn now_ms() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}
