//! UI child-process launch, shutdown, and entry points.

mod child;
mod flyout;

use windows::Win32::Foundation::CloseHandle;
use windows::Win32::System::Threading::{
    OpenProcess, TerminateProcess, WaitForSingleObject, PROCESS_SYNCHRONIZE, PROCESS_TERMINATE,
};

use super::{control_center, onboarding, settings_window};

pub(crate) use child::{
    current_child_handle, launch_onboarding, launch_settings, terminate_children,
};
pub(crate) use flyout::{reap_ui_child, toggle_control_center};

use child::PARENT_PID_ENV;

/// Stop the old background tray before the updater launches the new executable.
pub(crate) fn terminate_parent_for_update() {
    let Some(pid) = std::env::var(PARENT_PID_ENV)
        .ok()
        .and_then(|value| value.parse::<u32>().ok())
    else {
        return;
    };
    unsafe {
        let access = PROCESS_TERMINATE | PROCESS_SYNCHRONIZE;
        if let Ok(process) = OpenProcess(access, false, pid) {
            let _ = TerminateProcess(process, 0);
            let _ = WaitForSingleObject(process, 5_000);
            let _ = CloseHandle(process);
        }
    }
}

/// Run the only Settings window until it closes, then end the child process.
pub(crate) fn run_settings(check_update: bool) {
    if check_update {
        settings_window::open_and_check_updates();
    } else {
        settings_window::open();
    }
    let _ = slint::run_event_loop();
}

/// Run the only Onboarding window until it closes.
pub(crate) fn run_onboarding() {
    onboarding::open();
    let _ = slint::run_event_loop();
}

/// Run the Control Center flyout until dismissed.
pub(crate) fn run_control_center() {
    let code = control_center::open();
    std::process::exit(i32::from(code));
}
