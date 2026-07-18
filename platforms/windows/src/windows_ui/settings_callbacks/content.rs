use std::cell::RefCell;

use slint::{ComponentHandle, Model};

use crate::compose::FieldComposer;
use crate::{commands, shell, Compose, SettingsWindow};

use super::super::{models, settings_window};

thread_local! {
    static COMPOSER: RefCell<FieldComposer> = RefCell::new(FieldComposer::new());
}

pub(super) fn wire(window: &SettingsWindow) {
    wire_apps(window);
    wire_shortcuts(window);
    wire_composer(window);
}

fn wire_apps(window: &SettingsWindow) {
    let weak = window.as_weak();
    window.on_add_app(move |id| {
        if let Some(app) = shell::recent_apps()
            .into_iter()
            .find(|app| app.id == id.as_str())
        {
            commands::add_excluded_app(app);
        }
        if let Some(window) = weak.upgrade() {
            settings_window::refresh_apps(&window);
        }
    });

    let weak = window.as_weak();
    window.on_remove_app(move |id| {
        commands::remove_excluded_app(&id);
        if let Some(window) = weak.upgrade() {
            settings_window::refresh_apps(&window);
        }
    });
}

fn wire_shortcuts(window: &SettingsWindow) {
    let weak = window.as_weak();
    window.on_add_shortcut(move || {
        commands::add_shortcut();
        if let Some(window) = weak.upgrade() {
            window.set_shortcuts(models::shortcuts(&shell::shortcuts()));
        }
    });

    let weak = window.as_weak();
    window.on_remove_shortcut(move |index| {
        commands::remove_shortcut(index.max(0) as usize);
        if let Some(window) = weak.upgrade() {
            window.set_shortcuts(models::shortcuts(&shell::shortcuts()));
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
    let Some(window) = weak.upgrade() else { return };
    let model = window.get_shortcuts();
    if let Some(mut entry) = model.row_data(index) {
        if trigger {
            entry.trigger = text
        } else {
            entry.expansion = text
        }
        model.set_row_data(index, entry);
    }
}

fn wire_composer(window: &SettingsWindow) {
    let compose = window.global::<Compose>();
    compose.on_reset(|text| {
        let (method, tone) = shell::method_and_tone();
        COMPOSER.with(|composer| composer.borrow_mut().reset(text.as_str(), method, tone));
    });
    compose.on_key(|text| {
        let character = text.chars().next().unwrap_or('\0');
        COMPOSER
            .with(|composer| composer.borrow_mut().key(character))
            .into()
    });
    compose.on_backspace(|| {
        COMPOSER
            .with(|composer| composer.borrow_mut().backspace())
            .into()
    });
}
