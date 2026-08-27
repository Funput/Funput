//! Navigation destinations and the import / export footer.
//! A new page is one [`Destination`] variant plus a `ViewStack` child.

pub(super) mod about;
mod destination;
mod tools;
mod transfer;

use adw::prelude::*;
use adw::{ActionRow, NavigationSplitView, ViewStack, WindowTitle};
use gtk::{ListBox, Orientation, SelectionMode};

pub(in crate::settings_window) use destination::Destination;

/// Switch the content pane and header. Overview status rows and the sidebar
/// list both call this so a later shortcut (step 4) can reuse the same path.
pub(super) fn show(
    stack: &ViewStack,
    split: &NavigationSplitView,
    title: &WindowTitle,
    dest: Destination,
) {
    stack.set_visible_child_name(dest.id());
    title.set_title(dest.title());
    if let Some(page) = split.content() {
        page.set_title(dest.title());
    }
    split.set_show_content(true);
}

pub(super) fn widget(
    stack: &ViewStack,
    split: &NavigationSplitView,
    window: &adw::Window,
    title: &WindowTitle,
) -> gtk::Widget {
    let list = nav_list(stack, split, window, title);
    let list_sync = list.clone();
    stack.connect_visible_child_name_notify(move |stack| {
        sync_list(&list_sync, stack);
    });

    let root = gtk::Box::new(Orientation::Vertical, 0);
    list.set_vexpand(true);
    root.append(&list);
    root.append(&gtk::Separator::new(Orientation::Horizontal));
    root.append(&tools::widget(window));
    root.upcast()
}

fn sync_list(list: &ListBox, stack: &ViewStack) {
    let Some(name) = stack.visible_child_name() else {
        return;
    };
    let mut i = 0;
    while let Some(row) = list.row_at_index(i) {
        if row.widget_name() == name.as_str() {
            if list.selected_row().as_ref() != Some(&row) {
                list.select_row(Some(&row));
            }
            return;
        }
        i += 1;
    }
}

fn nav_list(
    stack: &ViewStack,
    split: &NavigationSplitView,
    window: &adw::Window,
    title: &WindowTitle,
) -> ListBox {
    let list = ListBox::new();
    list.add_css_class("navigation-sidebar");
    list.set_selection_mode(SelectionMode::Single);
    for dest in Destination::ALL {
        let row = ActionRow::builder()
            .title(dest.title())
            .activatable(true)
            .name(dest.id())
            .build();
        row.add_prefix(&gtk::Image::from_icon_name(dest.icon()));
        list.append(&row);
        // Chuyển mã sits with the pages because that is where people look for it,
        // but it is not one: it opens a window of its own. Linux has no tray, so
        // this row and `funput-convert.desktop` are what stand in for the item
        // Windows puts there.
        if dest == Destination::Shortcuts {
            list.append(&tools::convert_row(window));
        }
    }

    let stack = stack.clone();
    let split = split.clone();
    let title = title.clone();
    list.connect_row_selected(move |_, row| {
        let Some(row) = row else { return };
        let Some(dest) = Destination::from_id(&row.widget_name()) else {
            return;
        };
        show(&stack, &split, &title, dest);
    });
    if let Some(first) = list.row_at_index(0) {
        list.select_row(Some(&first));
    }
    list
}
