//! Sidebar rows that *do* something rather than go somewhere: the converter, and
//! import / export.

use adw::ActionRow;
use adw::prelude::*;
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

/// The Chuyển mã row: looks like a destination, acts like a button.
///
/// **Not selectable, and that is the whole trick.** The list selects one row and the
/// content pane follows it; a row with no page behind it would leave the sidebar
/// highlighting one thing while the pane shows another. A non-selectable row still
/// activates, so the highlight stays on whichever page is actually open.
pub(super) fn convert_row(window: &adw::Window) -> ActionRow {
    let row = ActionRow::builder()
        .title("Chuyển mã")
        .activatable(true)
        .selectable(false)
        .build();
    row.add_prefix(&gtk::Image::from_icon_name(
        "accessories-character-map-symbolic",
    ));
    let parent = window.clone();
    row.connect_activated(move |_| {
        // Through the same door the launcher uses, so there is one path in and one
        // place that decides whether to reuse the window already open.
        if let Some(app) = parent
            .application()
            .and_then(|app| app.downcast::<adw::Application>().ok())
        {
            crate::convert::present(&app);
        }
    });
    row
}
