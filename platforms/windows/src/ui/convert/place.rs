//! Where the Chuyển mã window opens: centred on the monitor under the cursor.
//!
//! `show()` alone lets Windows pick a spot, which reads as random. The bound is
//! the work area — screen minus taskbar — of the monitor the pointer is on, never
//! the primary screen. A monitor left of the primary has negative coordinates.

use raw_window_handle::{HasWindowHandle, RawWindowHandle};
use slint::PhysicalPosition;
use windows::Win32::Foundation::{HWND, POINT, RECT};
use windows::Win32::Graphics::Gdi::HMONITOR;
use windows::Win32::UI::HiDpi::{GetDpiForMonitor, MDT_EFFECTIVE_DPI};
use windows::Win32::UI::WindowsAndMessaging::{
    GetCursorPos, GetWindowRect, USER_DEFAULT_SCREEN_DPI,
};

use crate::shared::monitor;

/// Preferred size from `convert/window.slint`, used only when the window has not
/// reported one yet. Logical pixels, scaled like the Control Center flyout.
const FALLBACK_WIDTH: f64 = 760.0;
const FALLBACK_HEIGHT: f64 = 560.0;

pub(super) fn center(window: &slint::Window) {
    let cursor = cursor();
    let display = monitor::at(cursor);
    // `Window::size` excludes the frame, and `set_position` places the frame.
    // Centering the client box leaves the title bar half a caption high. The
    // outer rect includes that chrome once the native window exists.
    let (width, height) = outer_size(window).unwrap_or_else(|| pixel_size(window, display));
    let (x, y) = origin(monitor::work_area(display), width, height);
    window.set_position(PhysicalPosition::new(x, y));
}

/// Outer size in physical pixels, frame included. `None` before the HWND exists.
fn outer_size(window: &slint::Window) -> Option<(i32, i32)> {
    let handle = window.window_handle();
    let borrowed = handle.window_handle().ok()?;
    let RawWindowHandle::Win32(win) = borrowed.as_raw() else {
        return None;
    };
    let mut rect = RECT::default();
    unsafe { GetWindowRect(HWND(win.hwnd.get() as *mut _), &mut rect) }.ok()?;
    let width = rect.right.saturating_sub(rect.left);
    let height = rect.bottom.saturating_sub(rect.top);
    (width > 0 && height > 0).then_some((width, height))
}

fn cursor() -> POINT {
    let mut point = POINT::default();
    if unsafe { GetCursorPos(&mut point) }.is_ok() {
        return point;
    }
    point
}

/// Physical pixels. Once the native window exists, Slint's size already includes
/// its scale, so it is not multiplied again. The fallback is the path where that
/// size is still zero.
fn pixel_size(window: &slint::Window, display: HMONITOR) -> (i32, i32) {
    let size = window.size();
    if let (Ok(width), Ok(height)) = (i32::try_from(size.width), i32::try_from(size.height))
        && width > 0
        && height > 0
    {
        return (width, height);
    }
    let scale = scale_of(display);
    (
        (FALLBACK_WIDTH * scale).round() as i32,
        (FALLBACK_HEIGHT * scale).round() as i32,
    )
}

/// Physical pixels per logical one, or what `SLINT_SCALE_FACTOR` says. Same rule
/// as the Control Center flyout (`placement::scale_at`): Slint honours that
/// variable too. `1.0` when Windows cannot say.
fn scale_of(display: HMONITOR) -> f64 {
    if let Some(scale) = scale_override(std::env::var("SLINT_SCALE_FACTOR").ok().as_deref()) {
        return scale;
    }
    let (mut dpi_x, mut dpi_y) = (0, 0);
    match unsafe { GetDpiForMonitor(display, MDT_EFFECTIVE_DPI, &mut dpi_x, &mut dpi_y) } {
        Ok(()) if dpi_x > 0 => f64::from(dpi_x) / f64::from(USER_DEFAULT_SCREEN_DPI),
        _ => 1.0,
    }
}

/// `SLINT_SCALE_FACTOR` parsed the way Slint parses it: a positive number, or none.
fn scale_override(value: Option<&str>) -> Option<f64> {
    value
        .and_then(|raw| raw.parse::<f32>().ok())
        .filter(|scale| *scale > 0.0)
        .map(f64::from)
}

/// Top-left of a `width` × `height` window centred in `work`, physical pixels.
///
/// A window larger than the work area keeps its top-left on screen, which is
/// where the title bar is.
pub(super) fn origin(work: RECT, width: i32, height: i32) -> (i32, i32) {
    (
        centered(work.left, work.right, width),
        centered(work.top, work.bottom, height),
    )
}

fn centered(low: i32, high: i32, extent: i32) -> i32 {
    let span = high.saturating_sub(low);
    if extent >= span {
        return low;
    }
    low + (span - extent) / 2
}

#[cfg(test)]
mod tests {
    use super::*;

    fn work(left: i32, top: i32, width: i32, height: i32) -> RECT {
        RECT {
            left,
            top,
            right: left + width,
            bottom: top + height,
        }
    }

    #[test]
    fn it_centres_on_a_1080p_work_area() {
        let (x, y) = origin(work(0, 0, 1920, 1080), 760, 560);
        assert_eq!((x, y), ((1920 - 760) / 2, (1080 - 560) / 2));
    }

    #[test]
    fn it_centres_on_a_monitor_left_of_the_primary() {
        let (x, y) = origin(work(-1920, 0, 1920, 1080), 760, 560);
        assert_eq!(x, -1920 + (1920 - 760) / 2);
        assert!(x < 0, "x={x} crossed onto the primary");
        assert_eq!(y, (1080 - 560) / 2);
    }

    #[test]
    fn a_window_larger_than_the_work_area_keeps_its_top_left_visible() {
        let (x, y) = origin(work(10, 40, 800, 600), 2000, 2000);
        assert_eq!((x, y), (10, 40));
    }
}
