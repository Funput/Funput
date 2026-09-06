//! Funput Linux — Settings, Onboarding, and the Chuyển mã window (GTK4 + libadwaita).
//!
//! Typing is handled by the Fcitx5 addon (`platforms/linux/fcitx5`) or the IBus
//! engine (`platforms/linux/ibus`); this binary edits the shared settings file
//! (`~/.config/Funput/settings.json`) and hosts the charset converter. No tray: VI/EN
//! toggling and the status icon are provided by Fcitx5/IBus themselves. Replaces the
//! retired Tauri shell.
//!
//! One binary, two launchers. `funput-settings.desktop` runs it plain;
//! `funput-convert.desktop` runs it with `--convert`. Both reach the *same* process,
//! which is what keeps one settings file and one instance.

#[cfg(not(target_os = "linux"))]
compile_error!(
    "funput-settings builds only on Linux (the typing engine ships as a Fcitx5/IBus shell)."
);

mod config_transfer;
mod convert;
mod onboarding;
mod settings;
mod settings_window;

use adw::Application;
use adw::prelude::*;
use gtk::gio;
use gtk::glib;

use crate::settings::Settings;

// Matches the bundle identifier used across platforms.
const APP_ID: &str = "app.funput.funput";

/// The flag the second launcher passes.
const CONVERT: &str = "--convert";

fn main() -> glib::ExitCode {
    // `HANDLES_COMMAND_LINE`, not the default flags, because the default ones do not
    // forward argv to the running instance at all — the primary only ever sees
    // `activate`, and `--convert` would vanish without a trace. `HANDLES_OPEN` is the
    // wrong tool too: it forwards *files*, not flags.
    let app = Application::builder()
        .application_id(APP_ID)
        .flags(gio::ApplicationFlags::HANDLES_COMMAND_LINE)
        .build();
    app.connect_command_line(|app, command_line| {
        let wants_convert = command_line
            .arguments()
            .iter()
            .any(|arg| arg == std::ffi::OsStr::new(CONVERT));
        route(app, wants_convert);
        0
    });
    // Still needed: a desktop file with `DBusActivatable`, or a session restoring the
    // app, activates it without ever going through a command line.
    app.connect_activate(|app| route(app, false));
    app.run()
}

fn route(app: &Application, wants_convert: bool) {
    if wants_convert {
        convert::present(app);
        return;
    }
    // Re-present the *settings* window, not whatever happens to be focused.
    // `active_window()` would raise the Chuyển mã window when it is the one in front,
    // so asking for Settings while converting would appear to do nothing at all.
    if let Some(window) = window_named(app, SETTINGS) {
        window.present();
        return;
    }
    if Settings::load().has_completed_onboarding {
        settings_window::build(app).present();
    } else {
        onboarding::build(app).present();
    }
}

/// The name Settings answers to. Onboarding and Settings are both `adw::Window` and
/// Chuyển mã is an `adw::ApplicationWindow`, so a downcast cannot tell the first two
/// apart — they say who they are instead.
pub(crate) const SETTINGS: &str = "settings";

/// The application's window with a given name, if one is open.
fn window_named(app: &Application, name: &str) -> Option<gtk::Window> {
    app.windows()
        .into_iter()
        .find(|window| window.widget_name() == name)
}
