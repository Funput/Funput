//! Control Center callback wiring.

use std::cell::RefCell;

use slint::ComponentHandle;

use crate::ControlCenterWindow;
use crate::shared::{commands, shell};
use funput_config::ToneStyle;

use super::{
    EXIT_DISMISS, EXIT_KEYBOARD, EXIT_SETTINGS, cycle_method, method_label, request_exit,
    status_line, tone_label,
};

thread_local! {
    /// Kept for the process. Dropping the timer stops it, and this process exits
    /// with the window, so nothing else has to.
    static SHELL_TIMER: RefCell<Option<slint::Timer>> = const { RefCell::new(None) };
}

pub(super) fn attach(window: &ControlCenterWindow) {
    // A hotkey updates the hook's file, not this process. The timer pulls that
    // into the switch. Queued for the event loop, same as the backdrop.
    watch_shell(window);
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

fn watch_shell(window: &ControlCenterWindow) {
    let weak = window.as_weak();
    let _ = slint::invoke_from_event_loop(move || {
        let tick = weak.clone();
        let timer = slint::Timer::default();
        timer.start(
            slint::TimerMode::Repeated,
            std::time::Duration::from_millis(250),
            move || apply_shell(&tick),
        );
        SHELL_TIMER.with(|slot| *slot.borrow_mut() = Some(timer));
    });
}

/// A hotkey wrote VI/EN into the file. Apply it when this process's copy differs.
/// A click that just saved matches the file, so the timer does not fight it.
fn apply_shell(weak: &slint::Weak<ControlCenterWindow>) {
    let Some(window) = weak.upgrade() else {
        return;
    };
    if !shell::reload_settings() {
        return;
    }
    let on = shell::enabled();
    window.set_enabled(on);
    window.set_status(status_line(on, shell::snapshot().method).into());
}
