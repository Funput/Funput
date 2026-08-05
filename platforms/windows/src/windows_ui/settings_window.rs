//! Settings window creation, state population, and external update state.

use std::cell::RefCell;

use slint::{ComponentHandle, Weak};

use super::models;
use super::settings_callbacks;
use crate::{commands, recorder, shell, system_accent, SettingsWindow, Theme};

thread_local! {
    static WINDOW: RefCell<Option<Weak<SettingsWindow>>> = const { RefCell::new(None) };
}

pub(super) fn open() {
    if let Some(window) = current() {
        system_accent::apply(&window.global::<Theme>());
        populate(&window);
        let _ = window.show();
        schedule_backdrop(window.as_weak());
        return;
    }

    let window = SettingsWindow::new().expect("create settings window");
    system_accent::apply(&window.global::<Theme>());
    populate(&window);
    settings_callbacks::wire(&window);
    let _ = window.show();
    schedule_backdrop(window.as_weak());
    WINDOW.with(|cell| *cell.borrow_mut() = Some(window.as_weak()));
    if let Ok(tab) = std::env::var("FUNPUT_SETTINGS_ACTIVE") {
        if !tab.is_empty() {
            window.set_active(tab.into());
        }
    }
}

/// Request a DWM backdrop once the loop has created the native window, then reveal
/// it by flipping the window to a transparent background. Deferred because the raw
/// handle is only valid after an event-loop turn, and the window must be created
/// opaque so Slint does not request a DirectComposition surface (which ignores Mica).
fn schedule_backdrop(weak: Weak<SettingsWindow>) {
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(window) = weak.upgrade() {
            let active = crate::mica::apply(window.window());
            window.global::<Theme>().set_mica(active);
        }
    });
}

fn current() -> Option<SettingsWindow> {
    WINDOW.with(|cell| cell.borrow().as_ref().and_then(Weak::upgrade))
}

pub(super) fn populate(window: &SettingsWindow) {
    let settings = shell::snapshot();
    window.set_method(settings.method.id().into());
    window.set_tone_style(settings.tone_style.id().into());
    match &settings.toggle_combo {
        // A recorded combo overrides the preset — the segmented shows "custom".
        Some(combo) => {
            window.set_hotkey("custom".into());
            window.set_hotkey_caps(models::combo_caps(combo));
            window.set_hotkey_conflict(recorder::system_conflict(combo));
        }
        None => {
            window.set_hotkey(settings.toggle_hotkey.id().into());
            window.set_hotkey_caps(models::caps(settings.toggle_hotkey));
            window.set_hotkey_conflict(false);
        }
    }
    match &settings.flip_combo {
        Some(combo) => {
            window.set_flip_hotkey("custom".into());
            window.set_flip_hotkey_caps(models::combo_caps(combo));
            window.set_flip_hotkey_conflict(recorder::system_conflict(combo));
        }
        None => {
            window.set_flip_hotkey(settings.flip_hotkey.id().into());
            window.set_flip_hotkey_caps(models::flip_caps(settings.flip_hotkey));
            window.set_flip_hotkey_conflict(false);
        }
    }
    window.set_smart_restore(settings.smart_restore);
    window.set_eager_restore(settings.eager_restore);
    window.set_spell_check(settings.spell_check);
    window.set_auto_capitalize(settings.auto_capitalize);
    window.set_launch_at_login(settings.launch_at_login);
    window.set_version(env!("CARGO_PKG_VERSION").into());
    window.set_update_state("idle".into());
    window.set_update_version("".into());
    window.set_update_message("".into());
    refresh_apps(window);
    window.set_shortcuts(models::shortcuts(&shell::shortcuts()));
}

/// Reflect an asynchronous updater step if Settings is still open.
pub(crate) fn set_update_state(state: &str, version: &str, message: &str) {
    if let Some(window) = current() {
        window.set_update_state(state.into());
        window.set_update_version(version.into());
        window.set_update_message(message.into());
    }
}

pub(super) fn open_and_check_updates() {
    open();
    if let Some(window) = current() {
        window.set_active("about".into());
    }
    commands::check_for_updates();
}

pub(super) fn refresh_apps(window: &SettingsWindow) {
    let excluded = shell::excluded_apps();
    let addable = shell::recent_apps()
        .into_iter()
        .filter(|recent| !excluded.iter().any(|item| item.id == recent.id))
        .collect::<Vec<_>>();
    window.set_excluded_apps(models::apps(&excluded));
    window.set_recent_apps(models::apps(&addable));
}
