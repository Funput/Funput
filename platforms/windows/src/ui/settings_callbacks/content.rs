use std::cell::RefCell;

use slint::{ComponentHandle, Model};

use crate::shared::{commands, shell};
use funput_desktop::{FieldComposer, FieldView};

use crate::{Compose, FieldEdit, SettingsWindow};

use super::super::models;

thread_local! {
    static COMPOSER: RefCell<FieldComposer> = RefCell::new(FieldComposer::new());
}

pub(super) fn wire(window: &SettingsWindow) {
    wire_shortcuts(window);
    wire_composer(window);
}

fn wire_shortcuts(window: &SettingsWindow) {
    refresh_shortcuts_ui(window);

    let weak = window.as_weak();
    window.on_add_shortcut(move || {
        commands::add_shortcut();
        if let Some(window) = weak.upgrade() {
            refresh_shortcuts_ui(&window);
        }
    });

    let weak = window.as_weak();
    window.on_remove_shortcut(move |index| {
        commands::remove_shortcut(index.max(0) as usize);
        if let Some(window) = weak.upgrade() {
            refresh_shortcuts_ui(&window);
        }
    });

    let weak = window.as_weak();
    window.on_edit_trigger(move |index, text| {
        update_shortcut(&weak, index, text, true);
    });

    let weak = window.as_weak();
    window.on_edit_expansion(move |index, text| {
        update_shortcut(&weak, index, text, false);
    });

    let weak = window.as_weak();
    window.on_prune_shortcuts(move || {
        commands::prune_incomplete_shortcuts();
        if let Some(window) = weak.upgrade() {
            refresh_shortcuts_ui(&window);
        }
    });
}

fn refresh_shortcuts_ui(window: &SettingsWindow) {
    window.set_shortcuts(models::shortcuts(&shell::shortcuts()));
    window.set_can_add_shortcut(commands::can_add_shortcut());
}

fn update_shortcut(
    weak: &slint::Weak<SettingsWindow>,
    index: i32,
    text: slint::SharedString,
    trigger: bool,
) {
    let index = index.max(0) as usize;
    if trigger {
        commands::set_shortcut_trigger(index, text.to_string());
    } else {
        commands::set_shortcut_expansion(index, text.to_string());
    }
    let Some(window) = weak.upgrade() else {
        return;
    };
    let model = window.get_shortcuts();
    if let Some(mut entry) = model.row_data(index) {
        if trigger {
            entry.trigger = text
        } else {
            entry.expansion = text
        }
        model.set_row_data(index, entry);
    }
    // A trigger edit can change what other rows say too (a repeat appears or goes),
    // so every row's note is refreshed, each in place to keep the field's focus.
    for (row, note) in funput_desktop::shortcut_notes(&shell::shortcuts())
        .into_iter()
        .enumerate()
    {
        if let Some(mut entry) = model.row_data(row)
            && models::set_note(&mut entry, note)
        {
            model.set_row_data(row, entry);
        }
    }
    window.set_can_add_shortcut(commands::can_add_shortcut());
}

fn wire_composer(window: &SettingsWindow) {
    let compose = window.global::<Compose>();
    compose.on_reset(|| {
        let settings = shell::snapshot();
        COMPOSER.with(|composer| composer.borrow_mut().reset(&settings));
    });
    compose.on_key(|text, cursor, anchor, typed| {
        let character = typed.chars().next().unwrap_or('\0');
        let field = field_view(&text, cursor, anchor);
        COMPOSER.with(|composer| to_slint(composer.borrow_mut().key(field, character)))
    });
    compose.on_backspace(|text, cursor, anchor| {
        let field = field_view(&text, cursor, anchor);
        COMPOSER.with(|composer| to_slint(Some(composer.borrow_mut().backspace(field))))
    });
}

/// Slint hands over byte offsets as `int`; a negative one means the start.
fn field_view(text: &str, cursor: i32, anchor: i32) -> FieldView<'_> {
    let offset = |value: i32| usize::try_from(value).unwrap_or(0);
    FieldView {
        text,
        cursor: offset(cursor),
        anchor: offset(anchor),
    }
}

/// `None` is a key that is no text: the TextInput handles it itself.
fn to_slint(edit: Option<funput_desktop::FieldEdit>) -> FieldEdit {
    edit.map_or_else(FieldEdit::default, |edit| FieldEdit {
        accepted: true,
        text: edit.text.into(),
        cursor: i32::try_from(edit.cursor).unwrap_or(i32::MAX),
    })
}
