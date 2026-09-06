//! Control Center callback wiring.

use slint::ComponentHandle;

use crate::ControlCenterWindow;
use crate::shared::{commands, shell};
use funput_config::ToneStyle;

use super::{
    EXIT_DISMISS, EXIT_KEYBOARD, EXIT_SETTINGS, cycle_method, method_label, request_exit,
    status_line, tone_label,
};

pub(super) fn attach(window: &ControlCenterWindow) {
    let weak = window.as_weak();
    window.on_toggle_enabled(move |on| {
        shell::set_enabled(on);
        if let Some(w) = weak.upgrade() {
            w.set_enabled(on);
            w.set_status(status_line(on, shell::snapshot().method).into());
        }
    });

    let weak = window.as_weak();
    window.on_cycle_method(move || {
        let next = cycle_method(shell::snapshot().method);
        commands::set_method(next);
        if let Some(w) = weak.upgrade() {
            w.set_method_label(method_label(next).into());
            w.set_status(status_line(shell::enabled(), next).into());
        }
    });

    let weak = window.as_weak();
    window.on_toggle_spell(move || {
        let on = !shell::snapshot().spell_check;
        commands::set_spell_check(on);
        if let Some(w) = weak.upgrade() {
            w.set_spell_check(on);
        }
    });

    let weak = window.as_weak();
    window.on_toggle_cap(move || {
        let on = !shell::snapshot().auto_capitalize;
        commands::set_auto_capitalize(on);
        if let Some(w) = weak.upgrade() {
            w.set_auto_capitalize(on);
        }
    });

    let weak = window.as_weak();
    window.on_toggle_restore(move || {
        let on = !shell::snapshot().smart_restore;
        commands::set_smart_restore(on);
        if let Some(w) = weak.upgrade() {
            w.set_smart_restore(on);
        }
    });

    let weak = window.as_weak();
    window.on_cycle_tone(move || {
        let next = match shell::snapshot().tone_style {
            ToneStyle::Traditional => ToneStyle::Modern,
            ToneStyle::Modern => ToneStyle::Traditional,
        };
        commands::set_tone_style(next);
        if let Some(w) = weak.upgrade() {
            w.set_tone_label(tone_label(next).into());
        }
    });

    window.on_open_keyboard(|| request_exit(EXIT_KEYBOARD));
    window.on_open_settings(|| request_exit(EXIT_SETTINGS));
    window.on_dismiss(|| request_exit(EXIT_DISMISS));
}
