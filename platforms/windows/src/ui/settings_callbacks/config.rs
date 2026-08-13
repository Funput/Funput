//! The Dữ liệu page: export, import, and bringing a UniKey table over.
//!
//! The file pickers stay native — that is the one dialog Windows should own — but
//! results are reported through the window's own [`Notice`](../../../ui/shell/notice.slint)
//! overlay rather than `rfd::MessageDialog`, which draws a system message box with
//! none of this window's Fluent tokens and no idea about the Mica backdrop.

use slint::ComponentHandle;

use crate::shared::commands;
use crate::SettingsWindow;
use funput_config::transfer::{self, ImportSummary};

use super::super::settings_window;

pub(super) fn wire(window: &SettingsWindow) {
    let weak = window.as_weak();
    window.on_export_config(move || {
        let Some(path) = rfd::FileDialog::new()
            .set_file_name(format!("Funput-config-{}.json", transfer::today_stamp()))
            .add_filter("JSON", &["json"])
            .save_file()
        else {
            return;
        };
        let Some(window) = weak.upgrade() else {
            return;
        };
        match commands::export_config(&path) {
            Ok(()) => notice(&window, "Đã xuất cấu hình", &shown_path(&path), false),
            Err(err) => notice(&window, "Không xuất được cấu hình", &err.to_string(), true),
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
        let result = commands::import_config(&path);
        let Some(window) = weak.upgrade() else {
            return;
        };
        match result {
            Ok(summary) => {
                settings_window::populate(&window);
                notice(&window, "Đã nhập cấu hình", &import_body(&summary), false);
            }
            Err(err) => notice(&window, "Không nhập được cấu hình", &err.to_string(), true),
        }
    });

    let weak = window.as_weak();
    window.on_import_unikey(move || {
        // UniKey writes `ukmacro.txt`, so `.txt` is the filter — but the file can
        // be named anything once the user has copied it somewhere, hence the
        // second, unrestricted entry.
        let Some(path) = rfd::FileDialog::new()
            .set_file_name("ukmacro.txt")
            .add_filter("Bảng gõ tắt UniKey", &["txt"])
            .add_filter("Tất cả tệp", &["*"])
            .pick_file()
        else {
            return;
        };
        let result = commands::import_unikey_macros(&path);
        let Some(window) = weak.upgrade() else {
            return;
        };
        match result {
            Ok(summary) => {
                settings_window::populate(&window);
                notice(&window, "Đã nhập từ UniKey", &unikey_body(&summary), false);
            }
            Err(err) => notice(
                &window,
                "Không nhập được bảng gõ tắt",
                &err.to_string(),
                true,
            ),
        }
    });
}

/// Raise the in-window notice. `danger` is for a failure the user has to read, not
/// a summary they can wave away.
fn notice(window: &SettingsWindow, title: &str, body: &str, danger: bool) {
    window.set_notice_title(title.into());
    window.set_notice_body(body.into());
    window.set_notice_danger(danger);
    window.set_notice_open(true);
}

/// Just the file name: the full path is the user's own choice from a dialog they
/// just dismissed, and spelling it out crowds the notice for no new information.
fn shown_path(path: &std::path::Path) -> String {
    path.file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .unwrap_or_else(|| path.display().to_string())
}

/// A macro file carries only gõ tắt, so this says nothing about typing options —
/// unlike [`import_body`], which reports a whole config document. Zero of both
/// means every row was already present with the same text.
fn unikey_body(summary: &ImportSummary) -> String {
    if summary.shortcuts_added == 0 && summary.shortcuts_updated == 0 {
        return "Bảng gõ tắt đã có sẵn các mục này, không có gì thay đổi.".to_string();
    }
    format!(
        "Gõ tắt: thêm {}, cập nhật {}.",
        summary.shortcuts_added, summary.shortcuts_updated
    )
}

fn import_body(summary: &ImportSummary) -> String {
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
