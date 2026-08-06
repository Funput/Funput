use slint::ComponentHandle;

use funput_config::transfer::{self, ImportSummary};
use crate::{commands, SettingsWindow};

use super::super::settings_window;

pub(super) fn wire(window: &SettingsWindow) {
    window.on_export_config(|| {
        let Some(path) = rfd::FileDialog::new()
            .set_file_name(format!(
                "Funput-config-{}.json",
                transfer::today_stamp()
            ))
            .add_filter("JSON", &["json"])
            .save_file()
        else {
            return;
        };
        if let Err(err) = commands::export_config(&path) {
            message(&format!("Không xuất được cấu hình: {err}"));
        }
    });

    let weak = window.as_weak();
    window.on_import_config(move || {
        let Some(path) = rfd::FileDialog::new()
            .add_filter("JSON", &["json"])
            .pick_file()
        else {
            return;
        };
        match commands::import_config(&path) {
            Ok(summary) => {
                if let Some(window) = weak.upgrade() {
                    settings_window::populate(&window);
                }
                message(&import_message(&summary));
            }
            Err(err) => message(&err.to_string()),
        }
    });
}

fn message(text: &str) {
    rfd::MessageDialog::new()
        .set_title("Cấu hình")
        .set_description(text)
        .show();
}

fn import_message(summary: &ImportSummary) -> String {
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
