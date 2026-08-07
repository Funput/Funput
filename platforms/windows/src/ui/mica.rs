//! Windows 11 Mica (or Windows 10 Acrylic) backdrop for the Slint windows.
//!
//! Slint has no backdrop API, so we hand the raw window handle — exposed by the
//! `raw-window-handle-06` feature — to `window-vibrancy`, which sets the DWM
//! attributes and extends the frame so DWM composites the material behind the
//! window's transparent pixels.
//!
//! Three conditions make this work and are each easy to get wrong:
//!   * The Skia renderer. FemtoVG's GL surface carries no alpha, so transparent
//!     pixels turn black instead of revealing the backdrop.
//!   * No winit "transparent" window. That path uses DirectComposition, which the
//!     system backdrop ignores — the window just turns see-through to the desktop.
//!   * Order of operations. The window must be *created opaque* (so Slint does not
//!     request a composited surface), then made transparent only after the backdrop
//!     is applied. Hence apply runs from the event loop, and the caller flips
//!     the transparent background afterwards.

/// Long-lived window backdrop: Mica first, Acrylic fallback.
pub fn apply(window: &slint::Window) -> bool {
    #[cfg(windows)]
    {
        use window_vibrancy::{apply_acrylic, apply_mica};
        let handle = window.window_handle();
        if apply_mica(&handle, None).is_ok() {
            return true;
        }
        if apply_acrylic(&handle, None).is_ok() {
            return true;
        }
    }
    let _ = window;
    false
}

/// Transient flyout backdrop (Control Center): Acrylic first, then Mica, plus
/// the small Win11 corner radius menus and flyouts use.
#[cfg(windows)]
pub fn apply_flyout(window: &slint::Window) -> bool {
    use window_vibrancy::{apply_acrylic, apply_mica};

    let handle = window.window_handle();
    let ok = apply_acrylic(&handle, None).is_ok() || apply_mica(&handle, None).is_ok();
    round_flyout(&handle);
    ok
}

#[cfg(not(windows))]
pub fn apply_flyout(_window: &slint::Window) -> bool {
    false
}

/// Soften the `no-frame` HWND. Slint has no window-radius API; Win11 DWM does.
#[cfg(windows)]
fn round_flyout(handle: &slint::WindowHandle) {
    use raw_window_handle::{HasWindowHandle, RawWindowHandle};
    use windows::Win32::Foundation::HWND;
    use windows::Win32::Graphics::Dwm::{
        DwmSetWindowAttribute, DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_ROUNDSMALL,
        DWM_WINDOW_CORNER_PREFERENCE,
    };

    let Ok(borrowed) = handle.window_handle() else {
        return;
    };
    let RawWindowHandle::Win32(win) = borrowed.as_raw() else {
        return;
    };
    let hwnd = HWND(win.hwnd.get() as *mut _);
    let preference = DWMWCP_ROUNDSMALL;
    let _ = unsafe {
        DwmSetWindowAttribute(
            hwnd,
            DWMWA_WINDOW_CORNER_PREFERENCE,
            std::ptr::from_ref(&preference).cast(),
            std::mem::size_of::<DWM_WINDOW_CORNER_PREFERENCE>() as u32,
        )
    };
}
