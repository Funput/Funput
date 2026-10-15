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

/// The program that draws the Windows shell: the taskbar, the tray and its overflow
/// popup, the desktop, Alt-Tab and Task View.
const EXPLORER: &str = "explorer.exe";

/// The only `explorer.exe` windows that are an app: File Explorer, where people
/// really do type Vietnamese — renaming a file, filling the search box.
///
/// An allowlist, because the shell surfaces it draws are open-ended. The old list
/// of shell classes was always one short: the tray's overflow popup was missing, so
/// opening it replayed the pin File Explorer had been given and undid a choice just
/// made in the Control Center (#549). Every surface the trip to the tray passes
/// through must be invisible to per-app memory, or the act of making a choice there
/// undoes it. An `explorer.exe` window not listed here costs nothing worse than
/// following the global VI/EN state.
const EXPLORER_APPS: [&str; 2] = ["CabinetWClass", "ExploreWClass"];

/// Whether a window of program `exe` and class `class` is the shell rather than an
/// app — decided by both, since the pin is keyed by program but explorer draws both.
pub(super) fn is_shell_surface(exe: &str, class: &str) -> bool {
    exe == EXPLORER && !EXPLORER_APPS.contains(&class)
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

    fn shell(class: &str) -> bool {
        is_shell_surface(EXPLORER, class)
    }

    /// Measured on Windows 11: clicking the tray icon puts `Shell_TrayWnd` in the
    /// foreground, which is the whole reason the guard exists.
    #[test]
    fn the_taskbar_is_the_shell() {
        assert!(shell("Shell_TrayWnd"));
        assert!(shell("Shell_SecondaryTrayWnd"));
    }

    /// #549: the tray's ^ popup — Windows 11, then Windows 10 — replayed File
    /// Explorer's pin right after a choice made in the Control Center.
    #[test]
    fn the_tray_overflow_is_the_shell() {
        assert!(shell("TopLevelWindowForOverflowXamlIsland"));
        assert!(shell("NotifyIconOverflowWindow"));
    }

    #[test]
    fn the_desktop_and_the_switchers_are_too() {
        assert!(shell("Progman"));
        assert!(shell("WorkerW"));
        assert!(shell("ForegroundStaging"));
        assert!(shell("MultitaskingViewFrame"));
        assert!(shell("XamlExplorerHostIslandWindow")); // Windows 11 Alt-Tab
    }

    /// explorer.exe also draws real windows, and those are apps like any other.
    #[test]
    fn a_file_explorer_window_is_an_app() {
        assert!(!shell("CabinetWClass"));
        assert!(!shell("ExploreWClass"));
    }

    #[test]
    fn ordinary_windows_are_apps() {
        assert!(!is_shell_surface("chrome.exe", "Chrome_WidgetWin_1"));
        assert!(!is_shell_surface("notepad.exe", "Notepad"));
    }

    /// Only explorer draws the shell: a borrowed class name does not make another
    /// program's window part of it.
    #[test]
    fn another_program_with_a_shell_like_class_is_an_app() {
        assert!(!is_shell_surface("notepad.exe", "WorkerW"));
    }

    /// A window whose program could not be read is turned away before this is
    /// asked; one whose class could not be read is an app unless explorer drew it.
    #[test]
    fn an_unreadable_class_is_not_the_shell_elsewhere() {
        assert!(!is_shell_surface("code.exe", ""));
    }
}
