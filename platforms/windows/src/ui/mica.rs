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

/// Long-lived window backdrop (Settings, Onboarding): Mica, or nothing.
///
/// There is deliberately no Acrylic fallback here. `apply_mica` is Win11-only, but
/// below it `apply_acrylic` still *succeeds*, through the undocumented
/// `SetWindowCompositionAttribute` path — and that path is wrong for a window the
/// user drags and resizes:
///   * `window-vibrancy` documents it as making the window "lag when resizing or
///     dragging" on Windows 10 v1903+, and DWM re-blurs the desktop behind every
///     frame, so the whole window feels sluggish and not just while moving.
///   * It is handed no tint (alpha is forced to 1/255), so it blurs without
///     covering anything.
///
/// Reporting success would then flip `Theme.mica`, and the UI paints `transparent`
/// and drops its own `Backdrop` — leaving a see-through, laggy window. Returning
/// false is exactly what routes Windows 10 to the opaque fallback the shells
/// already carry.
///
/// The flyout is a different case and keeps its Acrylic — see [`apply_flyout`].
pub fn apply(window: &slint::Window) -> bool {
    #[cfg(windows)]
    {
        if window_vibrancy::apply_mica(window.window_handle(), None).is_ok() {
            return true;
        }
    }
    let _ = window;
    false
}

/// Transient flyout backdrop (Control Center): Acrylic first, then Mica, plus
/// the small Win11 corner radius menus and flyouts use.
///
/// Acrylic stays here even on Windows 10, where [`apply`] refuses it: the flyout
/// is `no-frame` and fixed-size, so the resize/drag lag has nothing to act on, and
/// its own layer (`Theme.flyout-base`) is ~92% opaque, so an untinted blur behind
/// it cannot make it see-through.
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
        DWM_WINDOW_CORNER_PREFERENCE, DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_ROUNDSMALL,
        DwmSetWindowAttribute,
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
