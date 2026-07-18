use adw::prelude::*;
use adw::{ActionRow, AlertDialog, PreferencesGroup, PreferencesWindow};
use gtk::gio;

use crate::config_transfer::{self, ImportSummary};
use crate::settings::Settings;

pub(super) fn group(window: &PreferencesWindow) -> PreferencesGroup {
    let group = PreferencesGroup::builder()
        .title("Sao lưu & khôi phục")
        .build();
    let export = action(
        "Xuất cấu hình",
        "Lưu gõ tắt và tuỳ chọn ra tệp .json.",
        "Xuất…",
    );
    let weak = window.downgrade();
    export.1.connect_clicked(move |_| {
        if let Some(window) = weak.upgrade() {
            export_dialog(&window);
        }
    });
    group.add(&export.0);
    let import = action(
        "Nhập cấu hình",
        "Gộp gõ tắt và áp tuỳ chọn từ tệp .json.",
        "Nhập…",
    );
    let weak = window.downgrade();
    import.1.connect_clicked(move |_| {
        if let Some(window) = weak.upgrade() {
            import_dialog(&window);
        }
    });
    group.add(&import.0);
    group
}

fn action(title: &str, subtitle: &str, label: &str) -> (ActionRow, gtk::Button) {
    let row = ActionRow::builder().title(title).subtitle(subtitle).build();
    let button = gtk::Button::builder()
        .label(label)
        .valign(gtk::Align::Center)
        .build();
    row.add_suffix(&button);
    row.set_activatable_widget(Some(&button));
    (row, button)
}

fn json_filter() -> gtk::FileFilter {
    let filter = gtk::FileFilter::new();
    filter.set_name(Some("JSON"));
    filter.add_suffix("json");
    filter
}

fn export_dialog(window: &PreferencesWindow) {
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

fn import_dialog(window: &PreferencesWindow) {
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

fn finish_import(window: &PreferencesWindow, settings: Settings, summary: &ImportSummary) {
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

fn alert(window: &PreferencesWindow, text: &str) {
    let dialog = AlertDialog::new(Some("Cấu hình"), Some(text));
    dialog.add_response("ok", "OK");
    dialog.present(Some(window));
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
        lines.push("Đã áp phím tắt và danh sách app bỏ qua.".to_string());
    }
    if summary.newer_version {
        lines.push("Lưu ý: tệp từ phiên bản mới hơn — một số mục có thể bị bỏ qua.".to_string());
    }
    lines.join("\n")
}
