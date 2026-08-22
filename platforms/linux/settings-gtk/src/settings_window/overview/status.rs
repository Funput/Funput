//! Activatable status rows: current method and shortcut count.

use adw::prelude::*;
use adw::{ActionRow, NavigationSplitView, PreferencesGroup, ViewStack, WindowTitle};

use crate::settings::Settings;

use super::super::sidebar::{self, Destination};

pub(super) fn group(
    stack: &ViewStack,
    split: &NavigationSplitView,
    title: &WindowTitle,
) -> PreferencesGroup {
    let settings = Settings::load();
    let group = PreferencesGroup::builder().title("Trạng thái").build();
    group.add(&navigate_row(
        "Phương thức",
        settings.method.label(),
        Destination::Typing,
        stack,
        split,
        title,
    ));
    group.add(&navigate_row(
        "Gõ tắt",
        &settings.shortcuts.len().to_string(),
        Destination::Shortcuts,
        stack,
        split,
        title,
    ));
    group
}

fn navigate_row(
    title_text: &str,
    subtitle: &str,
    dest: Destination,
    stack: &ViewStack,
    split: &NavigationSplitView,
    title: &WindowTitle,
) -> ActionRow {
    let row = ActionRow::builder()
        .title(title_text)
        .subtitle(subtitle)
        .activatable(true)
        .build();
    row.add_suffix(&gtk::Image::from_icon_name("go-next-symbolic"));
    let stack = stack.clone();
    let split = split.clone();
    let title = title.clone();
    row.connect_activated(move |_| sidebar::show(&stack, &split, &title, dest));
    row
}
