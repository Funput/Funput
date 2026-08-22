//! Import / export file dialogs. The sidebar owns the buttons; this module
//! talks to `config_transfer` and rebuilds Settings after a successful import.

use adw::prelude::*;
use adw::{AlertDialog, Toast, ToastOverlay, Window};
use gtk::gio;

use crate::config_transfer;
use crate::settings::Settings;

pub(super) fn export_dialog(window: &Window) {
    let dialog = gtk::FileDialog::builder()
        .title("Xuất cấu hình")
        .default_filter(&json_filter())
        .modal(true)
        .build();
    dialog.set_initial_name(Some(
        format!("Funput-config-{}.json", config_transfer::today_stamp()).as_str(),
    ));
    let weak = window.downgrade();
    dialog.save(Some(window), gio::Cancellable::NONE, move |result| {
        let Ok(path) = result.map(|file| file.path()) else {
            return;
        };
        let Some(path) = path else { return };
        let Some(window) = weak.upgrade() else { return };
        if let Err(error) = config_transfer::export_to(&path, &Settings::load()) {
            alert(&window, &format!("Không xuất được cấu hình: {error}"));
        } else {
            toast(&window, "Đã xuất cấu hình.");
        }
    });
}

pub(super) fn import_dialog(window: &Window) {
    let dialog = gtk::FileDialog::builder()
        .title("Nhập cấu hình")
        .default_filter(&json_filter())
        .modal(true)
        .build();
    let weak = window.downgrade();
    dialog.open(Some(window), gio::Cancellable::NONE, move |result| {
        let Ok(path) = result.map(|file| file.path()) else {
            return;
        };
        let Some(path) = path else { return };
        let Some(window) = weak.upgrade() else { return };
        let mut settings = Settings::load();
        match config_transfer::import_file(&path, &mut settings) {
            Ok(_) => finish_import(&window, settings),
            Err(error) => alert(&window, &error.to_string()),
        }
    });
}

fn finish_import(window: &Window, settings: Settings) {
    settings.save();
    if let Some(app) = window
        .application()
        .and_then(|app| app.downcast::<adw::Application>().ok())
    {
        let fresh = super::super::build(&app);
        fresh.present();
        window.close();
        toast(&fresh, "Đã nhập cấu hình.");
    } else {
        toast(window, "Đã nhập cấu hình.");
    }
}

fn toast(window: &Window, text: &str) {
    if let Some(overlay) = window.content().and_downcast::<ToastOverlay>() {
        overlay.add_toast(Toast::new(text));
    }
}

fn alert(window: &Window, text: &str) {
    let dialog = AlertDialog::new(Some("Cấu hình"), Some(text));
    dialog.add_response("ok", "OK");
    dialog.present(Some(window));
}

fn json_filter() -> gtk::FileFilter {
    let filter = gtk::FileFilter::new();
    filter.set_name(Some("JSON"));
    filter.add_suffix("json");
    filter
}
