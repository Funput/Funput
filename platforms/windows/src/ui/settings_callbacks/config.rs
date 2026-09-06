//! The Dữ liệu page: export, import, and bringing a UniKey table over.
//!
//! The file pickers stay native — that is the one dialog Windows should own — but
//! results are reported through the window's own [`Notice`](../../../ui/shell/notice.slint)
//! overlay rather than `rfd::MessageDialog`, which draws a system message box with
//! none of this window's Fluent tokens and no idea about the Mica backdrop.

mod message;

use slint::ComponentHandle;

use crate::SettingsWindow;
use crate::shared::commands;
use funput_config::transfer;

use message::{import_body, shown_path, unikey_body};

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

    // Opening the tool closes this window: one UI child at a time, which is the rule
    // Settings and the flyout already follow.
    window.on_open_convert(crate::ui::launch_convert);

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
            Ok((summary, charset)) => {
                settings_window::populate(&window);
                notice(
                    &window,
                    "Đã nhập từ UniKey",
                    &unikey_body(&summary, charset),
                    false,
                );
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
