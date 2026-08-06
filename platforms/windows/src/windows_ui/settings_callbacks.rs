//! Settings callbacks and live model synchronization.

use slint::ComponentHandle;

use funput_config::{FlipHotkey, Hotkey, Method, ToneStyle};
use crate::{commands, recorder, SettingsWindow};

use super::models;

mod config;
mod content;

pub(super) fn wire(window: &SettingsWindow) {
    let weak = window.as_weak();
    window.on_pick_method(move |value| {
        let Some(method) = Method::from_id(&value) else {
            return;
        };
        commands::set_method(method);
        if let Some(window) = weak.upgrade() {
            window.set_method(value);
        }
    });

    let weak = window.as_weak();
    window.on_pick_tone(move |value| {
        if let Some(tone) = ToneStyle::from_id(&value) {
            commands::set_tone_style(tone);
        }
        if let Some(window) = weak.upgrade() {
            window.set_tone_style(value);
        }
    });

    let weak = window.as_weak();
    window.on_pick_hotkey(move |value| {
        if let Some(hotkey) = Hotkey::from_id(&value) {
            commands::set_toggle_hotkey(hotkey);
            if let Some(window) = weak.upgrade() {
                window.set_hotkey(value);
                window.set_hotkey_caps(models::caps(hotkey));
                window.set_hotkey_conflict(false);
            }
        }
    });

    let weak = window.as_weak();
    window.on_pick_flip_hotkey(move |value| {
        if let Some(hotkey) = FlipHotkey::from_id(&value) {
            commands::set_flip_hotkey(hotkey);
            if let Some(window) = weak.upgrade() {
                window.set_flip_hotkey(value);
                window.set_flip_hotkey_caps(models::flip_caps(hotkey));
                window.set_flip_hotkey_conflict(false);
            }
        }
    });

    let weak = window.as_weak();
    window.on_record_combo(move |kind, text, ctrl, alt, shift, win| {
        let Some(recorded) = recorder::record(&text, ctrl, alt, shift, win) else {
            return false; // no Ctrl/Alt/Win, or no VK for this key — recorder shows the hint
        };
        let Some(window) = weak.upgrade() else {
            return false;
        };
        match kind.as_str() {
            "toggle" => {
                commands::set_toggle_combo(recorded.combo.clone());
                window.set_hotkey("custom".into());
                window.set_hotkey_caps(models::combo_caps(&recorded.combo));
                window.set_hotkey_conflict(recorded.system_conflict);
            }
            "flip" => {
                commands::set_flip_combo(recorded.combo.clone());
                window.set_flip_hotkey("custom".into());
                window.set_flip_hotkey_caps(models::combo_caps(&recorded.combo));
                window.set_flip_hotkey_conflict(recorded.system_conflict);
            }
            _ => return false,
        }
        true
    });

    window.on_set_smart(commands::set_smart_restore);
    window.on_set_eager(commands::set_eager_restore);
    window.on_set_spell(commands::set_spell_check);
    window.on_set_auto_cap(commands::set_auto_capitalize);
    window.on_set_launch(commands::set_launch_at_login);

    let weak = window.as_weak();
    window.on_set_enabled(move |on| {
        commands::set_enabled(on);
        if let Some(window) = weak.upgrade() {
            window.set_vi_enabled(on);
        }
    });

    content::wire(window);

    window.on_open_link(|url| commands::open_url(url.as_str()));
    window.on_check_update(commands::check_for_updates);
    window.on_install_update(commands::install_update);
    window.on_relaunch_now(commands::relaunch_after_update);

    config::wire(window);
}
