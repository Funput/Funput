//! The monitor under a point, and the part of it the taskbar leaves free.
//!
//! Shared because both processes place something next to the tray icon — the
//! background process its right-click menu, the UI process the Control Center
//! flyout — and both must stay off the taskbar.
//!
//! The bound is the work area of the monitor the point is on, never the primary
//! screen's: the taskbar can be on any display, and a monitor left of the primary
//! has negative coordinates, where a fixed origin would land on the wrong screen.

use windows::Win32::Foundation::{POINT, RECT};
use windows::Win32::Graphics::Gdi::{
    GetMonitorInfoW, HMONITOR, MONITOR_DEFAULTTONEAREST, MONITORINFO, MonitorFromPoint,
};
use windows::Win32::UI::WindowsAndMessaging::{SPI_GETWORKAREA, SystemParametersInfoW};

/// The monitor `point` is on, or the nearest one.
pub fn at(point: POINT) -> HMONITOR {
    unsafe { MonitorFromPoint(point, MONITOR_DEFAULTTONEAREST) }
}

/// Work area — screen minus taskbar — of `monitor`, in physical pixels.
///
/// Falls back to the primary monitor's, which `MONITOR_DEFAULTTONEAREST` already
/// makes all but unreachable: it only matters if `GetMonitorInfoW` itself fails.
pub fn work_area(monitor: HMONITOR) -> RECT {
    let mut info = MONITORINFO {
        cbSize: size_of::<MONITORINFO>() as u32,
        ..Default::default()
    };
    if unsafe { GetMonitorInfoW(monitor, &mut info) }.as_bool() {
        return info.rcWork;
    }
    primary_work_area()
}

fn primary_work_area() -> RECT {
    let mut rect = RECT::default();
    let ptr = std::ptr::from_mut(&mut rect).cast();
    let _ = unsafe { SystemParametersInfoW(SPI_GETWORKAREA, 0, Some(ptr), Default::default()) };
    rect
}
