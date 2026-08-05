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
//!     is applied. Hence [`apply`] runs from the event loop, and the caller flips
//!     the transparent background afterwards. See the `invoke_from_event_loop` sites.

/// Ask DWM for the best available backdrop. Returns whether one is now active, so
/// the caller can switch the window background to transparent to reveal it.
///
/// Mica (Windows 11) is preferred; Acrylic is the Windows 10 fallback. Must run
/// after the native window exists.
pub fn apply(window: &slint::Window) -> bool {
    #[cfg(windows)]
    {
        use window_vibrancy::{apply_acrylic, apply_mica};

        let handle = window.window_handle();
        // `dark: None` lets DWM match the system light/dark preference.
        if apply_mica(&handle, None).is_ok() {
            return true;
        }
        // Windows 10 has no Mica; Acrylic is the closest translucent equivalent.
        if apply_acrylic(&handle, None).is_ok() {
            return true;
        }
    }
    let _ = window;
    false
}
