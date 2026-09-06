//! Where the flyout sits: anchored to the tray icon, always fully on screen.
//!
//! Anchoring alone is not enough. A tray icon is near a corner by definition, so a
//! flyout centred on it runs off the edge — the icon sitting 120px from the right of
//! a 2560px screen put a 340px-wide flyout 62px past it, and Windows does not pull
//! it back. Every system flyout slides along the taskbar instead of hanging off, so
//! this does too.
//!
//! The bound is the *work area of the monitor the icon is on*, not the primary
//! screen: the taskbar can be on any display, and a monitor left of the primary has
//! negative coordinates, where clamping to a fixed origin would fling the flyout
//! onto the wrong screen.

use windows::Win32::Foundation::POINT;
use windows::Win32::Graphics::Gdi::{
    GetMonitorInfoW, HMONITOR, MONITOR_DEFAULTTONEAREST, MONITORINFO, MonitorFromPoint,
};
use windows::Win32::UI::WindowsAndMessaging::{SPI_GETWORKAREA, SystemParametersInfoW};

/// Breathing room kept between the flyout and both the icon and the screen edge.
const GAP: f64 = 8.0;

/// A rectangle in physical screen pixels — the units the tray reports and Slint
/// positions in.
#[derive(Clone, Copy, Debug, PartialEq)]
pub(super) struct Rect {
    pub left: f64,
    pub top: f64,
    pub right: f64,
    pub bottom: f64,
}

impl Rect {
    pub(super) fn from_size(left: f64, top: f64, width: f64, height: f64) -> Self {
        Self {
            left,
            top,
            right: left + width,
            bottom: top + height,
        }
    }
}

/// Top-left corner for a `width` x `height` flyout anchored to `tray`, kept inside
/// `work`.
///
/// Above the icon when there is room, below it when there is not — a taskbar at the
/// top of the screen leaves nothing above. Horizontally it starts centred on the
/// icon and then slides just far enough to fit.
pub(super) fn anchor(tray: Rect, work: Rect, width: f64, height: f64) -> (i32, i32) {
    let mut y = tray.top - height - GAP;
    if y < work.top + GAP {
        y = tray.bottom + GAP;
    }
    let x = (tray.left + tray.right) / 2.0 - width / 2.0;
    (
        fit(x, work.left, work.right, width).round() as i32,
        fit(y, work.top, work.bottom, height).round() as i32,
    )
}

/// Slide `start..start + extent` inside `low..high`, keeping [`GAP`] at whichever
/// edge it ends up against.
///
/// The low edge is applied last, so it wins for a flyout taller or wider than the
/// work area: what stays visible is then the top-left of it, which is where the
/// title and the toggle are.
fn fit(start: f64, low: f64, high: f64, extent: f64) -> f64 {
    start.min(high - extent - GAP).max(low + GAP)
}

/// Work area — screen minus taskbar — of the monitor under the middle of `tray`.
///
/// Falls back to the primary monitor's, which `MONITOR_DEFAULTTONEAREST` already
/// makes all but unreachable: it only matters if `GetMonitorInfoW` itself fails.
pub(super) fn work_area(tray: Rect) -> Rect {
    let point = POINT {
        x: ((tray.left + tray.right) / 2.0) as i32,
        y: ((tray.top + tray.bottom) / 2.0) as i32,
    };
    let monitor: HMONITOR = unsafe { MonitorFromPoint(point, MONITOR_DEFAULTTONEAREST) };
    let mut info = MONITORINFO {
        cbSize: size_of::<MONITORINFO>() as u32,
        ..Default::default()
    };
    if unsafe { GetMonitorInfoW(monitor, &mut info) }.as_bool() {
        let work = info.rcWork;
        return Rect {
            left: f64::from(work.left),
            top: f64::from(work.top),
            right: f64::from(work.right),
            bottom: f64::from(work.bottom),
        };
    }
    primary_work_area()
}

fn primary_work_area() -> Rect {
    let mut rect = windows::Win32::Foundation::RECT::default();
    let ptr = std::ptr::from_mut(&mut rect).cast();
    let _ = unsafe { SystemParametersInfoW(SPI_GETWORKAREA, 0, Some(ptr), Default::default()) };
    Rect {
        left: f64::from(rect.left),
        top: f64::from(rect.top),
        right: f64::from(rect.right),
        bottom: f64::from(rect.bottom),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A 2560x1440 screen with a 40px taskbar along the bottom.
    fn work() -> Rect {
        Rect::from_size(0.0, 0.0, 2560.0, 1400.0)
    }

    /// The tray icon, 24x24, `from_right` pixels in from the right screen edge.
    fn tray(from_right: f64) -> Rect {
        Rect::from_size(2560.0 - from_right, 1400.0, 24.0, 24.0)
    }

    #[test]
    fn it_sits_centred_above_the_icon_when_that_fits() {
        let (x, y) = anchor(tray(380.0), work(), 340.0, 351.0);
        assert_eq!((x, y), (2022, 1041));
    }

    #[test]
    fn it_slides_in_instead_of_running_off_the_right_edge() {
        // The reported bug: centred on an icon this close, the flyout reached 2622
        // on a 2560 screen and the last 62px were simply not drawn.
        let (x, _) = anchor(tray(120.0), work(), 340.0, 351.0);
        assert_eq!(x + 340, 2560 - GAP as i32);
        assert!(x < 2560 - 340);
    }

    #[test]
    fn it_slides_in_from_the_left_edge_too() {
        let left_tray = Rect::from_size(4.0, 1400.0, 24.0, 24.0);
        let (x, _) = anchor(left_tray, work(), 340.0, 351.0);
        assert_eq!(x, GAP as i32);
    }

    #[test]
    fn it_drops_below_the_icon_when_the_taskbar_is_at_the_top() {
        // Work area starts at y=40, icon at the very top: nothing fits above it.
        let top_work = Rect::from_size(0.0, 40.0, 2560.0, 1400.0);
        let top_tray = Rect::from_size(2440.0, 40.0, 24.0, 24.0);
        let (_, y) = anchor(top_tray, top_work, 340.0, 351.0);
        assert_eq!(y, (40.0 + 24.0 + GAP) as i32);
    }

    #[test]
    fn it_stays_on_a_monitor_that_sits_left_of_the_primary() {
        // Negative coordinates: clamping to a fixed origin would have thrown the
        // flyout onto the primary screen instead.
        let left_work = Rect::from_size(-1920.0, 0.0, 1920.0, 1040.0);
        let left_tray = Rect::from_size(-100.0, 1040.0, 24.0, 24.0);
        let (x, y) = anchor(left_tray, left_work, 340.0, 351.0);
        assert!(x >= -1920 + GAP as i32, "x={x} escaped the left monitor");
        assert!(x + 340 <= 0 - GAP as i32, "x={x} spilled onto the primary");
        assert_eq!(y, (1040.0 - 351.0 - GAP) as i32);
    }

    #[test]
    fn a_flyout_larger_than_the_work_area_keeps_its_top_left_visible() {
        let (x, y) = anchor(tray(120.0), work(), 4000.0, 2000.0);
        assert_eq!((x, y), (GAP as i32, GAP as i32));
    }
}
