//! Where a Funput window opens: centred on the monitor under the cursor.
//!
//! `show()` alone lets Windows pick a spot, cascading each new window from the
//! last, which reads as random. The bound is the work area — screen minus taskbar
//! — of the monitor the pointer is on, never the primary screen. A monitor left
//! of the primary has negative coordinates.

use raw_window_handle::{HasWindowHandle, RawWindowHandle};
use slint::{ComponentHandle, PhysicalPosition};
use windows::Win32::Foundation::{HWND, POINT, RECT};
use windows::Win32::UI::WindowsAndMessaging::{GetCursorPos, GetWindowRect};

use crate::shared::monitor;

/// Centre `component`'s window on the first event-loop turn, the first one where
/// its native window exists to be measured.
///
/// Only a weak handle waits in the queue, so a window closed before that turn is
/// skipped rather than kept alive.
pub(crate) fn center_once_shown<C: ComponentHandle + 'static>(component: &C) {
    let weak = component.as_weak();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(component) = weak.upgrade() {
            center(component.window());
        }
    });
}

fn center(window: &slint::Window) {
    let Some(hwnd) = hwnd(window) else {
        return; // no native window: leave it where Windows put it
    };
    // `set_position` places the frame, which is what `GetWindowRect` measures.
    // `Window::size` leaves the frame out and would sit the title bar off-centre.
    let Some(before) = outer_size(hwnd) else {
        return;
    };
    let work = monitor::work_area(monitor::at(cursor()));
    move_to(window, origin(work, before));
    // Arriving on a monitor with another DPI rescales the window during that move,
    // so its old size no longer centres. One more move at the new size stays on the
    // same monitor and cannot rescale again.
    if let Some(after) = outer_size(hwnd).filter(|after| *after != before) {
        move_to(window, origin(work, after));
    }
}

fn move_to(window: &slint::Window, (x, y): (i32, i32)) {
    window.set_position(PhysicalPosition::new(x, y));
}

fn hwnd(window: &slint::Window) -> Option<HWND> {
    let handle = window.window_handle();
    let RawWindowHandle::Win32(win) = handle.window_handle().ok()?.as_raw() else {
        return None;
    };
    Some(HWND(win.hwnd.get() as *mut _))
}

/// Outer size in physical pixels, frame included.
fn outer_size(hwnd: HWND) -> Option<(i32, i32)> {
    let mut rect = RECT::default();
    // SAFETY: `hwnd` is the live window Slint just handed out, and `rect` is a
    // valid out-pointer for the duration of the call.
    unsafe { GetWindowRect(hwnd, &mut rect) }.ok()?;
    let width = rect.right.saturating_sub(rect.left);
    let height = rect.bottom.saturating_sub(rect.top);
    (width > 0 && height > 0).then_some((width, height))
}

fn cursor() -> POINT {
    let mut point = POINT::default();
    // SAFETY: `point` is a valid out-pointer. On failure it stays at the origin,
    // which resolves to the primary monitor.
    let _ = unsafe { GetCursorPos(&mut point) };
    point
}

/// Top-left of a window of outer `size` centred in `work`, physical pixels.
///
/// A window larger than the work area keeps its top-left on screen, which is
/// where the title bar is.
fn origin(work: RECT, (width, height): (i32, i32)) -> (i32, i32) {
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
        let (x, y) = origin(work(0, 0, 1920, 1080), (760, 560));
        assert_eq!((x, y), ((1920 - 760) / 2, (1080 - 560) / 2));
    }

    #[test]
    fn it_centres_on_a_monitor_left_of_the_primary() {
        let (x, y) = origin(work(-1920, 0, 1920, 1080), (760, 560));
        assert_eq!(x, -1920 + (1920 - 760) / 2);
        assert!(x < 0, "x={x} crossed onto the primary");
        assert_eq!(y, (1080 - 560) / 2);
    }

    #[test]
    fn a_window_larger_than_the_work_area_keeps_its_top_left_visible() {
        let (x, y) = origin(work(10, 40, 800, 600), (2000, 2000));
        assert_eq!((x, y), (10, 40));
    }

    /// The second move after a DPI change: the window grew by 150 %, and its
    /// centre still lands on the work area's.
    #[test]
    fn a_rescaled_window_is_centred_at_its_new_size() {
        let area = work(-2560, 0, 2560, 1400);
        let middle = |(x, y): (i32, i32), (w, h): (i32, i32)| (x + w / 2, y + h / 2);
        let small = (760, 560);
        let large = (1140, 840);
        assert_eq!(
            middle(origin(area, small), small),
            middle(origin(area, large), large)
        );
        assert_eq!(middle(origin(area, large), large), (-2560 + 1280, 700));
    }
}
