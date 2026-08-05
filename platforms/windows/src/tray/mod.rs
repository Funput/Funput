//! Native tray icon + context menu on the keyboard-hook Win32 message loop.
//!
//! Phase C keeps left-click = toggle VI and right-click = native menu. Phase B
//! will swap left-click to an Acrylic popover without reshaping this module.

use std::cell::RefCell;

use funput_core::InputMethod;
use tray_icon::menu::CheckMenuItem;
use tray_icon::{TrayIcon, TrayIconBuilder};

use crate::{hook, shell};

mod events;
mod icon;
mod menu;
mod method_menu;

use menu::TrayMenu;
use method_menu::MethodMenu;

struct TrayState {
    tray: TrayIcon,
    methods: MethodMenu,
    enabled: CheckMenuItem,
}

thread_local! {
    static TRAY: RefCell<Option<TrayState>> = const { RefCell::new(None) };
}

/// Build the tray on the current keyboard-hook thread.
pub fn install() {
    let on = shell::enabled();
    let method = shell::method();
    let TrayMenu {
        menu,
        methods,
        enabled,
    } = menu::build(method, on);

    let tray = TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        // Left-click toggles VI/EN; the menu opens on right-click instead.
        .with_menu_on_left_click(false)
        .with_tooltip(icon::tooltip(on, method))
        .with_icon(icon::make_icon(on).expect("tray icon"))
        .build()
        .expect("build tray icon");

    TRAY.with(|c| {
        *c.borrow_mut() = Some(TrayState {
            tray,
            methods,
            enabled,
        })
    });

    // Hotkey / per-app auto-switch fire on this thread and keep the glyph honest.
    hook::set_on_toggle(refresh);
}

/// Drain pending tray + menu events after each `DispatchMessageW`.
pub fn drain_events() {
    events::drain();
}

/// Refresh tray fields after Settings (or another child) reloaded the config.
pub fn sync_from_shell() {
    let method = shell::method();
    sync_method(method);
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
            s.enabled.set_checked(on);
        }
    });
}

fn sync_method(method: InputMethod) {
    TRAY.with(|c| {
        if let Some(s) = c.borrow().as_ref() {
            s.methods.sync(method);
        }
    });
}
