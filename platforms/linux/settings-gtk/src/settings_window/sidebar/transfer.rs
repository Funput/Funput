//! Import / export file dialogs. The sidebar owns the buttons; this module
//! talks to `config_transfer` and rebuilds Settings after a successful import.

use adw::prelude::*;
use adw::{AlertDialog, Window};
use gtk::gio;

use crate::config_transfer::{self, ImportSummary};
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
        if let Err(error) = config_transfer::export_to(&path, &Settings::load()) {
            if let Some(window) = weak.upgrade() {
                alert(&window, &format!("Không xuất được cấu hình: {error}"));
            }
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
            Ok(summary) => finish_import(&window, settings, &summary),
            Err(error) => alert(&window, &error.to_string()),
        }
    });
}

fn finish_import(window: &Window, settings: Settings, summary: &ImportSummary) {
    settings.save();
    let text = summary_text(summary);
    if let Some(app) = window
        .application()
        .and_then(|app| app.downcast::<adw::Application>().ok())
    {
        let fresh = super::super::build(&app);
        fresh.present();
        window.close();
        alert(&fresh, &text);
    } else {
        alert(window, &text);
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

fn summary_text(summary: &ImportSummary) -> String {
    let mut lines = vec!["Đã áp các tuỳ chọn gõ.".to_string()];
    if summary.shortcuts_added > 0 || summary.shortcuts_updated > 0 {
        lines.push(format!(
            "Gõ tắt: thêm {}, cập nhật {}.",
            summary.shortcuts_added, summary.shortcuts_updated
        ));
    } else {
        lines.push("Không có gõ tắt mới.".to_string());
    }
    if summary.applied_platform {
        lines.push("Đã áp phím tắt Linux.".to_string());
    }
    if summary.newer_version {
        lines.push("Lưu ý: tệp từ phiên bản mới hơn — một số mục có thể bị bỏ qua.".to_string());
    }
    lines.join("\n")
}
