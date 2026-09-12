//! What a window is: which program drew it, what the toolkit calls it, and whether
//! it is a place a person types at all.
//!
//! Split out of [`super`] so the hook proc next door stays a list of decisions
//! rather than a mix of decisions and Win32 plumbing.

use std::sync::OnceLock;

use windows::Win32::Foundation::{CloseHandle, HWND};
use windows::Win32::System::Threading::{
    OpenProcess, PROCESS_NAME_WIN32, PROCESS_QUERY_LIMITED_INFORMATION, QueryFullProcessImageNameW,
};
use windows::Win32::UI::WindowsAndMessaging::{GetClassNameW, GetWindowThreadProcessId};
use windows::core::PWSTR;

static OWN_EXE_ID: OnceLock<String> = OnceLock::new();

/// Funput's own app id, so its windows can be told apart from everyone else's.
pub(super) fn own_exe_id() -> &'static String {
    OWN_EXE_ID.get_or_init(|| {
        std::env::current_exe()
            .ok()
            .and_then(|p| p.file_name().map(|n| n.to_string_lossy().to_lowercase()))
            .unwrap_or_else(|| "funput.exe".to_string())
    })
}

/// Window classes that belong to the Windows shell itself rather than to a program
/// the user switched to.
///
/// **The taskbar is the one that matters**, and it is the reason this list exists:
/// the tray icon lives on it, so every trip to Funput's own flyout goes through a
/// foreground change to `Shell_TrayWnd` — and if per-app memory answers that, the
/// user's choice is undone by the act of making it. The desktop is the same story
/// on a quieter path, and Alt-Tab and Task View flash past between two real windows.
///
/// All of them are drawn by `explorer.exe`, which is why this is decided by class
/// and not by program: a File Explorer window (`CabinetWClass`) is somewhere people
/// really do type Vietnamese — renaming a file, filling the search box — and it
/// keeps working like any other app.
///
/// A class missing from this list costs what the old behaviour cost, nothing worse,
/// so add one when a surface turns up rather than guessing at them now.
const SHELL_SURFACES: [&str; 6] = [
    // The taskbar, and the same on a second monitor.
    "Shell_TrayWnd",
    "Shell_SecondaryTrayWnd",
    // The desktop: the icon host, and the wallpaper window behind it.
    "Progman",
    "WorkerW",
    // Alt-Tab, and Task View.
    "ForegroundStaging",
    "MultitaskingViewFrame",
];

/// Whether this window is the shell rather than an app.
pub(super) fn is_shell_surface(class: &str) -> bool {
    SHELL_SURFACES.contains(&class)
}

/// A window's class name — what the toolkit that drew it calls itself, which is how
/// [`crate::background::inject::note_foreground`] recognizes a browser engine
/// without knowing the browser, and how [`is_shell_surface`] recognizes the taskbar.
/// Empty when the window is gone or has no class, which reads as "not a browser" and
/// "not the shell", both of which are the safe answers.
pub(super) fn class_of_window(hwnd: HWND) -> String {
    // Class names are capped at 256 characters by `RegisterClass`, so this cannot
    // truncate one that matters.
    let mut buf = [0u16; 257];
    // SAFETY: The API receives a writable slice and handles invalid window handles.
    let len = unsafe { GetClassNameW(hwnd, &mut buf) };
    String::from_utf16_lossy(&buf[..len.max(0) as usize])
}

/// Resolve a window's owning process to its app id — the lowercased exe file name
/// (e.g. "code.exe"), which is the key the per-app VI/EN memory uses.
pub(super) fn exe_of_window(hwnd: HWND) -> Option<String> {
    if hwnd.0.is_null() {
        return None;
    }
    let mut pid = 0u32;
    // SAFETY: pid is writable; an invalid or expired window returns failure.
    unsafe { GetWindowThreadProcessId(hwnd, Some(&mut pid)) };
    if pid == 0 {
        return None;
    }
    // SAFETY: Request query access only; failure is propagated without using a handle.
    let handle = unsafe { OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid) }.ok()?;

    let mut buf = [0u16; 260];
    let mut len = buf.len() as u32;
    // SAFETY: handle is open and buf has the capacity provided in len.
    let res = unsafe {
        QueryFullProcessImageNameW(
            handle,
            PROCESS_NAME_WIN32,
            PWSTR(buf.as_mut_ptr()),
            &mut len,
        )
    };
    // SAFETY: Release the owned process handle exactly once after its final use.
    let _ = unsafe { CloseHandle(handle) };
    res.ok()?;

    let full = String::from_utf16_lossy(&buf[..len as usize]);
    let file = full.rsplit(['\\', '/']).next().unwrap_or("");
    if file.is_empty() {
        return None;
    }
    Some(file.to_lowercase())
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Measured on Windows 11: clicking the tray icon puts `Shell_TrayWnd` in the
    /// foreground, which is the whole reason the guard exists.
    #[test]
    fn the_taskbar_is_the_shell() {
        assert!(is_shell_surface("Shell_TrayWnd"));
        assert!(is_shell_surface("Shell_SecondaryTrayWnd"));
    }

    #[test]
    fn the_desktop_and_the_switchers_are_too() {
        assert!(is_shell_surface("Progman"));
        assert!(is_shell_surface("WorkerW"));
        assert!(is_shell_surface("ForegroundStaging"));
        assert!(is_shell_surface("MultitaskingViewFrame"));
    }

    /// The point of deciding by class: explorer.exe also draws real windows, and
    /// those are apps like any other.
    #[test]
    fn a_file_explorer_window_is_an_app() {
        assert!(!is_shell_surface("CabinetWClass"));
    }

    #[test]
    fn ordinary_windows_are_apps() {
        assert!(!is_shell_surface("Chrome_WidgetWin_1"));
        assert!(!is_shell_surface("Notepad"));
    }

    /// A window whose class could not be read must not be mistaken for the shell —
    /// that would silently stop the per-app switch for a real app.
    #[test]
    fn an_unreadable_class_is_not_the_shell() {
        assert!(!is_shell_surface(""));
    }
}
