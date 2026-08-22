//! Import / export rows at the bottom of the sidebar.

use adw::prelude::*;
use adw::ActionRow;
use gtk::{ListBox, SelectionMode};

use super::transfer;

pub(super) fn widget(window: &adw::Window) -> ListBox {
    let list = ListBox::new();
    list.add_css_class("navigation-sidebar");
    list.set_selection_mode(SelectionMode::None);

    let import = tool_row("Nhập cấu hình", "document-open-symbolic");
    let export = tool_row("Xuất cấu hình", "document-save-symbolic");
    let parent = window.clone();
    import.connect_activated(move |_| transfer::import_dialog(&parent));
    let parent = window.clone();
    export.connect_activated(move |_| transfer::export_dialog(&parent));

    list.append(&import);
    list.append(&export);
    list
}

fn tool_row(title: &str, icon: &str) -> ActionRow {
    let row = ActionRow::builder().title(title).activatable(true).build();
    row.add_prefix(&gtk::Image::from_icon_name(icon));
    row
}
