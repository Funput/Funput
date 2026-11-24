//! The tray icon's size in pixels, and noticing when the taskbar wants another.
//!
//! The taskbar draws notification icons at `SM_CXSMICON` for its own DPI: 16 px
//! at 100 %, 20 at 125 %, 24 at 150 %. A bitmap of any other size gets resampled,
//! so [`super::glyph`] draws exactly this one. The DPI is that of the monitor
//! `Shell_TrayWnd` is on, since this process has no visible window to ask.
//!
//! A scale change reaches the tray's hidden window as `WM_DPICHANGED`,
//! `WM_DISPLAYCHANGE` or `WM_SETTINGCHANGE` (it is top-level, so it gets the
//! broadcasts), and an Explorer restart as `TaskbarCreated`. The window belongs to
//! `tray-icon`, so it is subclassed to see them. The subclass only posts to the
//! hook pump: the redraw sends messages back to this same window, and a broadcast
//! can arrive in the middle of anything that pumps sent messages.

use std::cell::Cell;
use std::sync::OnceLock;

use windows::Win32::Foundation::{HWND, LPARAM, LRESULT, POINT, WPARAM};
use windows::Win32::Graphics::Gdi::{
    HMONITOR, MONITOR_DEFAULTTOPRIMARY, MonitorFromPoint, MonitorFromWindow,
};
use windows::Win32::System::Threading::GetCurrentThreadId;
use windows::Win32::UI::HiDpi::{GetDpiForMonitor, GetSystemMetricsForDpi, MDT_EFFECTIVE_DPI};
use windows::Win32::UI::Shell::{DefSubclassProc, SetWindowSubclass};
use windows::Win32::UI::WindowsAndMessaging::{
    FindWindowW, PostThreadMessageW, RegisterWindowMessageW, SM_CXSMICON, USER_DEFAULT_SCREEN_DPI,
    WM_APP, WM_DISPLAYCHANGE, WM_DPICHANGED, WM_SETTINGCHANGE,
};
use windows::core::{PCWSTR, w};

/// Posted to the hook thread when the icon size may have changed; the pump in
/// `hook::run` hands it to [`super::rescale`]. `WM_APP + 1` is the toggle's.
pub const WM_RESCALE: u32 = WM_APP + 2;

const SUBCLASS_ID: usize = 0x46_54_52_59; // "FTRY"

thread_local! {
    /// A rescale is already queued. `WM_SETTINGCHANGE` comes in bursts, and one
    /// look at the end of a burst answers all of it.
    static QUEUED: Cell<bool> = const { Cell::new(false) };
}

/// The pixel size the taskbar draws a tray icon at right now.
pub(super) fn icon_px() -> u32 {
    // SAFETY: plain metric lookup.
    let px = unsafe { GetSystemMetricsForDpi(SM_CXSMICON, taskbar_dpi()) };
    u32::try_from(px).ok().filter(|&px| px > 0).unwrap_or(16)
}

fn taskbar_dpi() -> u32 {
    let (mut dpi_x, mut dpi_y) = (0, 0);
    // SAFETY: `taskbar_monitor` always yields a monitor handle; the outputs are
    // locals.
    match unsafe { GetDpiForMonitor(taskbar_monitor(), MDT_EFFECTIVE_DPI, &mut dpi_x, &mut dpi_y) }
    {
        Ok(()) if dpi_x > 0 => dpi_x,
        _ => USER_DEFAULT_SCREEN_DPI,
    }
}

/// The monitor holding the main taskbar, or the primary one while Explorer is
/// not running.
fn taskbar_monitor() -> HMONITOR {
    // SAFETY: lookups with no out-parameters; both fall back to the primary
    // monitor rather than failing.
    unsafe {
        match FindWindowW(w!("Shell_TrayWnd"), PCWSTR::null()) {
            Ok(taskbar) => MonitorFromWindow(taskbar, MONITOR_DEFAULTTOPRIMARY),
            Err(_) => MonitorFromPoint(POINT::default(), MONITOR_DEFAULTTOPRIMARY),
        }
    }
}

/// Start listening on the tray's hidden window. Call once, on the hook thread.
pub(super) fn watch(window: HWND) {
    // SAFETY: `window` is the tray's, created on this thread and alive for the
    // whole session, so it outlives the subclass.
    let _ = unsafe { SetWindowSubclass(window, Some(proc), SUBCLASS_ID, 0) };
}

/// The queued rescale is being handled; the next change may queue another.
pub(super) fn delivered() {
    QUEUED.with(|queued| queued.set(false));
}

unsafe extern "system" fn proc(
    window: HWND,
    message: u32,
    wparam: WPARAM,
    lparam: LPARAM,
    _id: usize,
    _data: usize,
) -> LRESULT {
    let rescale = matches!(message, WM_DPICHANGED | WM_DISPLAYCHANGE | WM_SETTINGCHANGE)
        || message == taskbar_created();
    if rescale && !QUEUED.with(|queued| queued.replace(true)) {
        // SAFETY: posting to this thread's own queue. If it fails the flag is
        // cleared so the next broadcast tries again.
        let posted =
            unsafe { PostThreadMessageW(GetCurrentThreadId(), WM_RESCALE, WPARAM(0), LPARAM(0)) };
        if posted.is_err() {
            delivered();
        }
    }
    // SAFETY: chaining on is what a subclass owes; `tray-icon` still has to see
    // every one of these, `TaskbarCreated` above all.
    unsafe { DefSubclassProc(window, message, wparam, lparam) }
}

/// The message Explorer broadcasts when the taskbar is (re)created.
fn taskbar_created() -> u32 {
    static ID: OnceLock<u32> = OnceLock::new();
    // SAFETY: registers or looks up a string; no pointers kept.
    *ID.get_or_init(|| unsafe { RegisterWindowMessageW(w!("TaskbarCreated")) })
}
