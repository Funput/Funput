//! "Chung" page: general preferences (launch at login) + config backup.

use adw::prelude::*;
use adw::{ActionRow, AlertDialog, PreferencesGroup, PreferencesPage, PreferencesWindow, SwitchRow};
use gtk::gio;

use crate::config_transfer::{self, ImportSummary};
use crate::settings::Settings;

pub(super) fn page(window: &PreferencesWindow) -> PreferencesPage {
    let s = Settings::load();
    let page = PreferencesPage::builder()
        .title("Chung")
        .icon_name("preferences-system-symbolic")
        .build();

    let group = PreferencesGroup::new();
    // On Linux the engine runs inside the fcitx5/ibus daemon, whose autostart is
    // managed by the desktop session — this toggle only persists the preference.
    let row = SwitchRow::builder()
        .title("Khởi động cùng phiên đăng nhập")
        .subtitle("Bộ gõ do desktop quản lý tự khởi động; tuỳ chọn này chỉ được lưu lại.")
        .active(s.launch_at_login)
        .build();
    row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|s| s.launch_at_login = on);
    });
    group.add(&row);
    page.add(&group);

    // --- Backup & restore ---
    let backup = PreferencesGroup::builder().title("Sao lưu & khôi phục").build();

    let export_row = ActionRow::builder()
        .title("Xuất cấu hình")
        .subtitle("Lưu gõ tắt và tuỳ chọn ra tệp .json.")
        .build();
    let export_btn = gtk::Button::builder().label("Xuất…").valign(gtk::Align::Center).build();
    let weak = window.downgrade();
    export_btn.connect_clicked(move |_| {
        if let Some(window) = weak.upgrade() {
            export_dialog(&window);
        }
    });
    export_row.add_suffix(&export_btn);
    export_row.set_activatable_widget(Some(&export_btn));
    backup.add(&export_row);

    let import_row = ActionRow::builder()
        .title("Nhập cấu hình")
        .subtitle("Gộp gõ tắt và áp tuỳ chọn từ tệp .json.")
        .build();
    let import_btn = gtk::Button::builder().label("Nhập…").valign(gtk::Align::Center).build();
    let weak = window.downgrade();
    import_btn.connect_clicked(move |_| {
        if let Some(window) = weak.upgrade() {
            import_dialog(&window);
        }
    });
    import_row.add_suffix(&import_btn);
    import_row.set_activatable_widget(Some(&import_btn));
    backup.add(&import_row);

    page.add(&backup);
    page
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
        // `Err` includes the user cancelling — nothing to report then.
        let Ok(path) = result.map(|f| f.path()) else { return };
        let Some(path) = path else { return };
        if let Err(err) = config_transfer::export_to(&path, &Settings::load()) {
            if let Some(window) = weak.upgrade() {
                alert(&window, &format!("Không xuất được cấu hình: {err}"));
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
        let Ok(path) = result.map(|f| f.path()) else { return };
        let Some(path) = path else { return };
        let Some(window) = weak.upgrade() else { return };

        let mut settings = Settings::load();
        match config_transfer::import_file(&path, &mut settings) {
            Ok(summary) => {
                settings.save();
                let text = summary_text(&summary);
                // Panes read their values once at build time, so rebuild the window
                // to reflect the imported settings. The engine reloads on its own
                // (inotify on settings.json).
                if let Some(app) = window.application().and_then(|a| a.downcast::<adw::Application>().ok()) {
                    let fresh = super::build(&app);
                    fresh.present();
                    window.close();
                    alert(&fresh, &text);
                } else {
                    alert(&window, &text);
                }
            }
            Err(err) => alert(&window, &err.to_string()),
        }
    });
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
