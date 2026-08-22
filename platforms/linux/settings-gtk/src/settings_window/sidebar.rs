//! Navigation destinations and the import / export / About footer.
//! A new page is one [`Destination`] variant plus a `ViewStack` child.

mod about;
mod transfer;

use adw::prelude::*;
use adw::{ActionRow, NavigationSplitView, ViewStack, WindowTitle};
use gtk::{ListBox, Orientation, SelectionMode};

#[derive(Clone, Copy, PartialEq, Eq)]
pub(super) enum Destination {
    Overview,
    Typing,
    Keyboard,
    Shortcuts,
}

impl Destination {
    pub(super) const ALL: [Self; 4] = [
        Self::Overview,
        Self::Typing,
        Self::Keyboard,
        Self::Shortcuts,
    ];

    pub(super) const fn id(self) -> &'static str {
        match self {
            Self::Overview => "overview",
            Self::Typing => "typing",
            Self::Keyboard => "keyboard",
            Self::Shortcuts => "shortcuts",
        }
    }

    pub(super) const fn title(self) -> &'static str {
        match self {
            Self::Overview => "Tổng quan",
            Self::Typing => "Cách gõ",
            Self::Keyboard => "Phím tắt",
            Self::Shortcuts => "Gõ tắt",
        }
    }

    const fn icon(self) -> &'static str {
        match self {
            Self::Overview => "preferences-system-symbolic",
            Self::Typing => "input-keyboard-symbolic",
            Self::Keyboard => "preferences-desktop-keyboard-shortcuts-symbolic",
            Self::Shortcuts => "edit-find-replace-symbolic",
        }
    }

    fn from_id(id: &str) -> Option<Self> {
        Self::ALL.into_iter().find(|dest| dest.id() == id)
    }
}

pub(super) fn widget(
    stack: &ViewStack,
    split: &NavigationSplitView,
    window: &adw::Window,
    title: &WindowTitle,
) -> gtk::Widget {
    let list = nav_list(stack, split, title);
    let footer = tool_footer(window);

    let root = gtk::Box::new(Orientation::Vertical, 0);
    list.set_vexpand(true);
    root.append(&list);
    root.append(&gtk::Separator::new(Orientation::Horizontal));
    root.append(&footer);
    root.upcast()
}

fn nav_list(stack: &ViewStack, split: &NavigationSplitView, title: &WindowTitle) -> ListBox {
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
    }

    let stack = stack.clone();
    let split = split.clone();
    let title = title.clone();
    list.connect_row_selected(move |_, row| {
        let Some(row) = row else { return };
        let Some(dest) = Destination::from_id(&row.widget_name()) else {
            return;
        };
        stack.set_visible_child_name(dest.id());
        title.set_title(dest.title());
        if let Some(page) = split.content() {
            page.set_title(dest.title());
        }
        split.set_show_content(true);
    });
    if let Some(first) = list.row_at_index(0) {
        list.select_row(Some(&first));
    }
    list
}

fn tool_footer(window: &adw::Window) -> ListBox {
    let list = ListBox::new();
    list.add_css_class("navigation-sidebar");
    list.set_selection_mode(SelectionMode::None);

    let import = tool_row("Nhập cấu hình", "document-open-symbolic");
    let export = tool_row("Xuất cấu hình", "document-save-symbolic");
    let about_row = tool_row("Giới thiệu", "help-about-symbolic");
    let parent = window.clone();
    import.connect_activated(move |_| transfer::import_dialog(&parent));
    let parent = window.clone();
    export.connect_activated(move |_| transfer::export_dialog(&parent));
    let parent = window.clone();
    about_row.connect_activated(move |_| about::present(&parent));

    list.append(&import);
    list.append(&export);
    list.append(&about_row);
    list
}

fn tool_row(title: &str, icon: &str) -> ActionRow {
    let row = ActionRow::builder().title(title).activatable(true).build();
    row.add_prefix(&gtk::Image::from_icon_name(icon));
    row
}
