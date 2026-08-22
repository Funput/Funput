//! About dialog and the Chung row that opens it. `present` stays callable from a
//! later sidebar so this page can go away without moving the dialog.

use adw::prelude::*;
use adw::{AboutDialog, ActionRow, PreferencesWindow};
use gtk::Image;

pub(super) fn row(window: &PreferencesWindow) -> ActionRow {
    let row = ActionRow::builder()
        .title("Giới thiệu Funput")
        .subtitle(format!("Phiên bản {}", env!("CARGO_PKG_VERSION")))
        .activatable(true)
        .build();
    row.add_suffix(&Image::from_icon_name("go-next-symbolic"));
    let parent = window.clone();
    row.connect_activated(move |_| present(&parent));
    row
}

pub(super) fn present(parent: &PreferencesWindow) {
    let about = AboutDialog::builder()
        .application_name("Funput")
        .application_icon("funput")
        .version(env!("CARGO_PKG_VERSION"))
        .comments("Bộ gõ tiếng Việt — miễn phí, mã nguồn mở.")
        .developer_name("Funput")
        .website("https://funput.app/")
        .issue_url("https://github.com/Funput/Funput/issues")
        .license_type(gtk::License::MitX11)
        .build();
    about.add_link("GitHub", "https://github.com/Funput/Funput");
    // AdwDialog presents itself relative to a parent widget (no transient/modal).
    about.present(Some(parent));
}
