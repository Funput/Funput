use std::cell::RefCell;
use std::rc::Rc;

use adw::prelude::*;
use adw::{EntryRow, ExpanderRow};
use gtk::{Align, Button};

use crate::settings::{Settings, Shortcut};

pub(super) fn build(
    index: usize,
    shortcut: &Shortcut,
    rebuild: &Rc<RefCell<Option<Rc<dyn Fn()>>>>,
) -> (ExpanderRow, EntryRow) {
    let expander = ExpanderRow::builder()
        .title(if shortcut.trigger.is_empty() {
            "Gõ tắt mới"
        } else {
            shortcut.trigger.as_str()
        })
        .subtitle(shortcut.expansion.as_str())
        .build();
    let trigger = EntryRow::builder().title("Chữ tắt").build();
    let expansion = EntryRow::builder().title("Bung thành").build();
    trigger.set_text(shortcut.trigger.as_str());
    expansion.set_text(shortcut.expansion.as_str());
    bind_edit(index, &trigger, expander.clone(), true);
    bind_edit(index, &expansion, expander.clone(), false);
    bind_focus(index, &trigger, true);
    bind_focus(index, &expansion, false);
    expander.add_row(&trigger);
    expander.add_row(&expansion);
    expander.add_suffix(&delete_button(index, rebuild));
    (expander, trigger)
}

fn bind_edit(index: usize, entry: &EntryRow, expander: ExpanderRow, trigger: bool) {
    entry.connect_changed(move |entry| {
        let text = entry.text().to_string();
        let display = text.clone();
        Settings::update(move |settings| {
            if let Some(item) = settings.shortcuts.get_mut(index) {
                if trigger {
                    item.trigger = text;
                } else {
                    item.expansion = text;
                }
            }
        });
        if trigger {
            expander.set_title(if display.is_empty() {
                "Gõ tắt mới"
            } else {
                &display
            });
        } else {
            expander.set_subtitle(&display);
        }
    });
}

fn bind_focus(index: usize, entry: &EntryRow, trigger: bool) {
    let entry = entry.clone();
    let focus = gtk::EventControllerFocus::new();
    focus.connect_leave(move |_| {
        let text = entry.text().to_string();
        Settings::update(move |settings| {
            if let Some(item) = settings.shortcuts.get_mut(index) {
                if trigger {
                    item.trigger = text;
                } else {
                    item.expansion = text;
                }
            }
        });
    });
    entry.add_controller(focus);
}

fn delete_button(index: usize, rebuild: &Rc<RefCell<Option<Rc<dyn Fn()>>>>) -> Button {
    let button = Button::builder()
        .icon_name("user-trash-symbolic")
        .valign(Align::Center)
        .tooltip_text("Xoá gõ tắt")
        .build();
    button.add_css_class("flat");
    let rebuild = rebuild.clone();
    button.connect_clicked(move |_| {
        Settings::update(move |settings| {
            if index < settings.shortcuts.len() {
                settings.shortcuts.remove(index);
            }
        });
        if let Some(callback) = rebuild.borrow().as_ref() {
            callback();
        }
    });
    button
}
