//! Native tray icon + context menu on the keyboard-hook Win32 message loop.
//!
//! Left-click toggles the Acrylic Control Center, double-click opens Settings,
//! right-click opens a thin menu.

use std::cell::RefCell;

use tray_icon::menu::{ContextMenu, Menu};
use tray_icon::{TrayIcon, TrayIconBuilder};
use windows::Win32::Foundation::{HWND, POINT};
use windows::Win32::UI::WindowsAndMessaging::HMENU;

use crate::background::hook;
use crate::shared::shell;

mod events;
mod icon;
mod menu;
mod popup;

struct TrayState {
    tray: TrayIcon,
    /// The same menu the tray holds, kept for its handle: the tray will not give
    /// it back, and [`show_menu`] opens it itself.
    menu: Menu,
}

thread_local! {
    static TRAY: RefCell<Option<TrayState>> = const { RefCell::new(None) };
}

/// Build the tray on the current keyboard-hook thread.
pub fn install() {
    let on = shell::enabled();
    let method = shell::method();
    let menu = menu::build();

    // The tray still takes the menu — that is what routes a chosen item back as a
    // `MenuEvent` — but it is told not to open it on either button: it would open
    // it over the taskbar (see `popup`).
    let tray = TrayIconBuilder::new()
        .with_menu(Box::new(menu.clone()))
        .with_menu_on_left_click(false)
        .with_menu_on_right_click(false)
        .with_tooltip(icon::tooltip(on, method))
        .with_icon(icon::make_icon(on).expect("tray icon"))
        .build()
        .expect("build tray icon");

    TRAY.with(|c| *c.borrow_mut() = Some(TrayState { tray, menu }));
    hook::set_on_toggle(refresh);
}

/// Open the right-click menu for a click at `cursor`, in physical screen pixels.
fn show_menu(cursor: POINT) {
    // Only the handles leave the cell. The menu runs a message loop of its own on
    // this thread, and the foreground-change callback delivered inside it calls
    // `sync_from_shell`, which borrows `TRAY` again.
    let handles = TRAY.with(|c| {
        let state = c.borrow();
        let state = state.as_ref()?;
        Some((state.tray.window_handle(), state.menu.hpopupmenu()))
    });
    if let Some((owner, menu)) = handles {
        popup::show(HWND(owner), HMENU(menu as *mut _), cursor);
    }
}

/// Drain pending tray + menu events after each `DispatchMessageW`.
pub fn drain_events() {
    events::drain();
}

/// Refresh tray fields after Settings (or another child) reloaded the config.
pub fn sync_from_shell() {
    refresh(shell::enabled());
}

fn refresh(on: bool) {
    let method = shell::method();
    TRAY.with(|c| {
        if let Some(s) = c.borrow().as_ref() {
            if let Some(glyph) = icon::make_icon(on) {
                let _ = s.tray.set_icon(Some(glyph));
            }
            let _ = s.tray.set_tooltip(Some(icon::tooltip(on, method)));
        }
    });
}
