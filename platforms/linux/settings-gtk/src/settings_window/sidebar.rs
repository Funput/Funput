//! Navigation destinations and the import / export footer.
//! A new page is one [`Destination`] variant plus a `ViewStack` child.

pub(super) mod about;
mod tools;
mod transfer;

use adw::prelude::*;
use adw::{ActionRow, NavigationSplitView, ViewStack, WindowTitle};
use gtk::{ListBox, Orientation, SelectionMode};

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

#[derive(Clone, Copy, PartialEq, Eq)]
pub(super) enum Destination {
    Overview,
    Typing,
    Keyboard,
    Shortcuts,
    About,
}

impl Destination {
    pub(super) const ALL: [Self; 5] = [
        Self::Overview,
        Self::Typing,
        Self::Keyboard,
        Self::Shortcuts,
        Self::About,
    ];

    pub(super) const fn id(self) -> &'static str {
        match self {
            Self::Overview => "overview",
            Self::Typing => "typing",
            Self::Keyboard => "keyboard",
            Self::Shortcuts => "shortcuts",
            Self::About => "about",
        }
    }

    pub(super) const fn title(self) -> &'static str {
        match self {
            Self::Overview => "Tổng quan",
            Self::Typing => "Cách gõ",
            Self::Keyboard => "Phím tắt",
            Self::Shortcuts => "Gõ tắt",
            Self::About => "Giới thiệu",
        }
    }

    pub(super) const fn icon(self) -> &'static str {
        match self {
            Self::Overview => "preferences-system-symbolic",
            Self::Typing => "input-keyboard-symbolic",
            Self::Keyboard => "preferences-desktop-keyboard-shortcuts-symbolic",
            Self::Shortcuts => "edit-find-replace-symbolic",
            Self::About => "help-about-symbolic",
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
        show(&stack, &split, &title, dest);
    });
    if let Some(first) = list.row_at_index(0) {
        list.select_row(Some(&first));
    }
    list
}

