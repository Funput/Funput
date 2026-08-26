//! Accepting files dropped onto the window.
//!
//! Slint 1.17 has a `DropArea`, but only for drags that start inside the
//! application — the winit backend never forwards the OS's own file drop. So the
//! window is told to accept files and its message loop is subclassed, which is the
//! one place this package reaches past Slint to Win32 for an input event.
//!
//! The HWND comes out of the Slint window the same way [`crate::ui::mica`] gets it.

use std::path::PathBuf;

use windows::Win32::Foundation::{HWND, LPARAM, LRESULT, WPARAM};
use windows::Win32::UI::Shell::{
    DefSubclassProc, DragAcceptFiles, DragFinish, DragQueryFileW, HDROP, SetWindowSubclass,
};
use windows::Win32::UI::WindowsAndMessaging::WM_DROPFILES;

/// Called on the UI thread with whatever was dropped.
type OnDrop = fn(Vec<PathBuf>);

thread_local! {
    static HANDLER: std::cell::Cell<Option<OnDrop>> = const { std::cell::Cell::new(None) };
}

const SUBCLASS_ID: usize = 0x46_55_4E_50; // "FUNP"

/// Start accepting dropped files on `window`, delivering them to `handler`.
///
/// Safe to call more than once: `SetWindowSubclass` with the same id replaces the
/// existing entry rather than stacking a second one.
pub(in crate::ui::convert) fn accept(window: &slint::Window, handler: OnDrop) {
    let Some(hwnd) = hwnd_of(window) else {
        return;
    };
    HANDLER.with(|cell| cell.set(Some(handler)));
    // SAFETY: `hwnd` belongs to this thread's window and outlives the subclass —
    // the process exits with the window.
    unsafe {
        DragAcceptFiles(hwnd, true);
        let _ = SetWindowSubclass(hwnd, Some(proc), SUBCLASS_ID, 0);
    }
}

fn hwnd_of(window: &slint::Window) -> Option<HWND> {
    use raw_window_handle::{HasWindowHandle, RawWindowHandle};
    let handle = window.window_handle();
    let borrowed = handle.window_handle().ok()?;
    match borrowed.as_raw() {
        RawWindowHandle::Win32(win) => Some(HWND(win.hwnd.get() as *mut _)),
        _ => None,
    }
}

/// Window procedure: take `WM_DROPFILES`, hand everything else back to Slint.
unsafe extern "system" fn proc(
    hwnd: HWND,
    message: u32,
    wparam: WPARAM,
    lparam: LPARAM,
    _id: usize,
    _data: usize,
) -> LRESULT {
    if message == WM_DROPFILES {
        // SAFETY: for WM_DROPFILES the OS passes an HDROP in wParam, and it stays
        // valid until DragFinish.
        let drop = HDROP(wparam.0 as *mut _);
        let paths = unsafe { collect(drop) };
        unsafe { DragFinish(drop) };
        if let Some(handler) = HANDLER.with(std::cell::Cell::get) {
            handler(paths);
        }
        return LRESULT(0);
    }
    // SAFETY: chaining to the next procedure is what a subclass owes; skipping it
    // would take every other message away from Slint.
    unsafe { DefSubclassProc(hwnd, message, wparam, lparam) }
}

/// Every path in the drop. Directories come through as their own path and are left
/// for the caller to expand or ignore.
///
/// # Safety
/// `drop` must be a live HDROP that `DragFinish` has not been called on.
unsafe fn collect(drop: HDROP) -> Vec<PathBuf> {
    // Index `0xFFFF_FFFF` asks for the count rather than a path.
    let count = unsafe { DragQueryFileW(drop, u32::MAX, None) };
    let mut paths = Vec::with_capacity(count as usize);
    for index in 0..count {
        // A zero-length buffer asks how long the path is, excluding the NUL.
        let len = unsafe { DragQueryFileW(drop, index, None) } as usize;
        if len == 0 {
            continue;
        }
        let mut buffer = vec![0u16; len + 1];
        // SAFETY: the buffer is one longer than the length just reported.
        let written = unsafe { DragQueryFileW(drop, index, Some(&mut buffer)) } as usize;
        paths.push(PathBuf::from(String::from_utf16_lossy(&buffer[..written])));
    }
    paths
}
