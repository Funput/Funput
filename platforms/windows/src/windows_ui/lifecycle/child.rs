//! UI child-process spawn, toggle, and reap.

use std::cell::RefCell;
use std::process::{Child, Command};
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

use tray_icon::Rect;

use super::control_center;
use crate::{shell, tray};

thread_local! {
    static UI_PROCESS: RefCell<Option<(UiKind, Child)>> = const { RefCell::new(None) };
}

/// Last Control Center dismiss (ms); debounce tray re-open after focus-loss.
static LAST_CC_DISMISS_MS: AtomicU64 = AtomicU64::new(0);

#[derive(Clone, Copy, PartialEq, Eq)]
pub(super) enum UiKind {
    Settings,
    Onboarding,
    ControlCenter,
}

pub(super) const RECENT_APPS_ENV: &str = "FUNPUT_RECENT_APPS";
pub(super) const PARENT_PID_ENV: &str = "FUNPUT_PARENT_PID";
const SETTINGS_ACTIVE_ENV: &str = "FUNPUT_SETTINGS_ACTIVE";
const TRAY_X: &str = "FUNPUT_TRAY_X";
const TRAY_Y: &str = "FUNPUT_TRAY_Y";
const TRAY_W: &str = "FUNPUT_TRAY_W";
const TRAY_H: &str = "FUNPUT_TRAY_H";

pub(crate) fn launch_settings(check_update: bool) {
    launch_settings_tab(if check_update { "about" } else { "typing" }, check_update);
}

fn launch_settings_tab(tab: &str, check_update: bool) {
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

pub(crate) fn toggle_control_center(rect: Rect) {
    reap_ui_child();
    let is_cc = UI_PROCESS.with(|cell| {
        cell.borrow()
            .as_ref()
            .is_some_and(|(kind, _)| *kind == UiKind::ControlCenter)
    });
    if is_cc {
        terminate_children();
        if shell::reload_settings() {
            tray::sync_from_shell();
        }
        return;
    }
    // Tray click unfocuses the flyout first; don't reopen in the same gesture.
    if now_ms().saturating_sub(LAST_CC_DISMISS_MS.load(Ordering::SeqCst)) < 400 {
        return;
    }
    terminate_children();
    let x = rect.position.x.to_string();
    let y = rect.position.y.to_string();
    let w = rect.size.width.to_string();
    let h = rect.size.height.to_string();
    spawn_child(
        "--control-center",
        UiKind::ControlCenter,
        &[(TRAY_X, &x), (TRAY_Y, &y), (TRAY_W, &w), (TRAY_H, &h)],
    );
}

pub(crate) fn reap_ui_child() {
    let finished = UI_PROCESS.with(|cell| {
        let mut slot = cell.borrow_mut();
        let Some((kind, child)) = slot.as_mut() else {
            return None;
        };
        match child.try_wait() {
            Ok(Some(status)) => {
                let out = (*kind, status.code().unwrap_or(0) as u8);
                *slot = None;
                Some(out)
            }
            _ => None,
        }
    });
    let Some((UiKind::ControlCenter, code)) = finished else {
        return;
    };
    if shell::reload_settings() {
        tray::sync_from_shell();
    }
    if code == control_center::EXIT_DISMISS {
        LAST_CC_DISMISS_MS.store(now_ms(), Ordering::SeqCst);
        return;
    }
    match code {
        control_center::EXIT_SETTINGS => launch_settings_tab("typing", false),
        control_center::EXIT_SHORTCUTS => launch_settings_tab("shortcuts", false),
        control_center::EXIT_KEYBOARD => launch_settings_tab("keyboard", false),
        _ => {}
    }
}

fn spawn_child(arg: &str, kind: UiKind, extra_env: &[(&str, &str)]) {
    let Some(exe) = std::env::current_exe().ok() else {
        return;
    };
    let recent = serde_json::to_string(&shell::recent_apps()).unwrap_or_else(|_| "[]".into());
    let mut cmd = Command::new(exe);
    cmd.arg(arg)
        .env(RECENT_APPS_ENV, recent)
        .env(PARENT_PID_ENV, std::process::id().to_string());
    for (key, value) in extra_env {
        cmd.env(*key, *value);
    }
    if let Some(child) = cmd.spawn().ok() {
        UI_PROCESS.with(|cell| *cell.borrow_mut() = Some((kind, child)));
    }
}

pub(crate) fn terminate_children() {
    UI_PROCESS.with(|cell| {
        if let Some((_, mut child)) = cell.borrow_mut().take() {
            let _ = child.kill();
            let _ = child.wait();
        }
    });
}

fn now_ms() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}
