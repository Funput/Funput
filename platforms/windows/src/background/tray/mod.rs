//! Native tray icon + context menu on the keyboard-hook Win32 message loop.
//!
//! Left-click toggles the Acrylic Control Center, double-click opens Settings,
//! right-click opens a thin menu.

use std::cell::RefCell;

use tray_icon::{TrayIcon, TrayIconBuilder};

use crate::background::hook;
use crate::shared::shell;

mod events;
mod icon;
mod menu;

struct TrayState {
    tray: TrayIcon,
}

thread_local! {
    static TRAY: RefCell<Option<TrayState>> = const { RefCell::new(None) };
}

/// Build the tray on the current keyboard-hook thread.
pub fn install() {
    let on = shell::enabled();
    let method = shell::method();
    let menu = menu::build();

    let tray = TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_menu_on_left_click(false)
        .with_tooltip(icon::tooltip(on, method))
        .with_icon(icon::make_icon(on).expect("tray icon"))
        .build()
        .expect("build tray icon");

    TRAY.with(|c| *c.borrow_mut() = Some(TrayState { tray }));
    hook::set_on_toggle(refresh);
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
