//! The foreground-window hook: the user switched apps.
//!
//! Two things follow from that. The caret is somewhere else entirely, so anything
//! Funput was composing is stale; and the new app may want a different VI/EN state
//! (see [`crate::shared::shell`]'s per-app auto-switch).

use std::sync::atomic::Ordering;
use std::sync::OnceLock;

use windows::core::PWSTR;
use windows::Win32::Foundation::{CloseHandle, HWND};
use windows::Win32::System::Threading::{
    OpenProcess, QueryFullProcessImageNameW, PROCESS_NAME_WIN32, PROCESS_QUERY_LIMITED_INFORMATION,
};
use windows::Win32::UI::Accessibility::HWINEVENTHOOK;
use windows::Win32::UI::WindowsAndMessaging::{GetWindowThreadProcessId, EVENT_SYSTEM_FOREGROUND};

use super::{FOREGROUND_IS_FUNPUT, ON_TOGGLE};
use crate::background::tray;
use crate::shared::shell;

static OWN_EXE_ID: OnceLock<String> = OnceLock::new();

/// Foreground-window changed: record the app and apply its per-app VI/EN default.
pub(super) unsafe extern "system" fn win_event_proc(
    _hook: HWINEVENTHOOK,
    event: u32,
    hwnd: HWND,
    _id_object: i32,
    _id_child: i32,
    _thread: u32,
    _time: u32,
) {
    if event != EVENT_SYSTEM_FOREGROUND {
        return;
    }
    let Some((id, name)) = exe_of_window(hwnd) else {
        return;
    };
    let is_funput = id == own_exe_id().as_str();
    FOREGROUND_IS_FUNPUT.store(is_funput, Ordering::Relaxed);
    // The caret is somewhere else entirely now, so nothing Funput has typed sits in
    // front of it any more (mirrors the mouse-click flush). Both directions: keys
    // typed into Funput's own windows compose in-process and never reach the engine.
    shell::clear();
    if is_funput {
        return;
    }

    // A Settings child persists changes to disk. Reload them as soon as focus
    // returns to a regular app, before the next keystroke reaches the engine.
    if shell::reload_settings() {
        tray::sync_from_shell();
    }
    shell::note_foreground(id.clone(), name);
    // Focus on a new app is the start of input: arm so the first letter is capitalized.
    shell::arm_capitalization();
    if let Some(on) = shell::apply_for_app(&id) {
        if let Some(cb) = ON_TOGGLE.get() {
            cb(on); // keep tray checkmark / tooltip in sync with the auto-switch
        }
    }
}

fn own_exe_id() -> &'static String {
    OWN_EXE_ID.get_or_init(|| {
        std::env::current_exe()
            .ok()
            .and_then(|p| p.file_name().map(|n| n.to_string_lossy().to_lowercase()))
            .unwrap_or_else(|| "funput.exe".to_string())
    })
}

/// Resolve a window's owning process to `(id, name)` where `id` is the lowercased
/// exe file name (e.g. "code.exe") and `name` strips the extension (e.g. "code").
unsafe fn exe_of_window(hwnd: HWND) -> Option<(String, String)> {
    if hwnd.0.is_null() {
        return None;
    }
    let mut pid = 0u32;
    GetWindowThreadProcessId(hwnd, Some(&mut pid));
    if pid == 0 {
        return None;
    }
    let handle = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid).ok()?;

    let mut buf = [0u16; 260];
    let mut len = buf.len() as u32;
    let res = QueryFullProcessImageNameW(
        handle,
        PROCESS_NAME_WIN32,
        PWSTR(buf.as_mut_ptr()),
        &mut len,
    );
    let _ = CloseHandle(handle);
    res.ok()?;

    let full = String::from_utf16_lossy(&buf[..len as usize]);
    let file = full.rsplit(['\\', '/']).next().unwrap_or("").to_string();
    if file.is_empty() {
        return None;
    }
    let id = file.to_lowercase();
    let name = id.strip_suffix(".exe").unwrap_or(&id).to_string();
    Some((id, name))
}
