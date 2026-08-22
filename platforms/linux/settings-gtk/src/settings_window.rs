//! The Settings window: an `AdwPreferencesWindow` with one page per destination.
//! Every control reads the current value from `settings.json` on build and writes
//! back through `Settings::update` on change — the engine reloads on its next
//! focus-in. A later step can swap this chrome for a split view; the page modules
//! stay the destinations.
//!
//! Each page lives in its own submodule under `settings_window/`.

mod general;
mod keyboard;
mod shortcuts;
mod typing;

use adw::prelude::*;
use adw::{Application, PreferencesWindow};

pub fn build(app: &Application) -> PreferencesWindow {
    let window = PreferencesWindow::builder()
        .title("Funput — Cài đặt")
        .default_width(640)
        .default_height(520)
        .build();
    window.set_application(Some(app));
    window.set_search_enabled(false);

    window.add(&typing::page());
    window.add(&keyboard::page());
    window.add(&shortcuts::page());
    window.add(&general::page(&window));

    window
}
