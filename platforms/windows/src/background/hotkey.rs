//! Which of the user's two hotkeys — the VI/EN toggle and the flip-word key — a
//! keyboard event just triggered.
//!
//! Hotkeys come in two shapes. One with a main key (Ctrl+`, Ctrl+Shift+V) fires
//! on that key's keydown with the exact modifier set. One made only of modifiers
//! (Ctrl+Shift, Alt+Shift) has no keydown to fire on and resolves when the
//! modifiers are released — see [`watcher`].

mod rules;
mod watcher;

use std::cell::RefCell;

use funput_desktop::Mods;
use windows::Win32::UI::Input::KeyboardAndMouse::VIRTUAL_KEY;

use crate::shared::shell;
use watcher::{Event, Watcher};

/// Which hotkey fired. The caller applies its own gating (flip only acts while
/// Vietnamese is on), so this stays a lookup.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Hit {
    Toggle,
    Flip,
}

thread_local! {
    /// The hook owns its own thread and message pump, so the in-flight gesture
    /// lives with that thread instead of behind a lock.
    static WATCHER: RefCell<Watcher> = RefCell::new(Watcher::default());
}

/// Match a keydown against the hotkeys that have a main key.
pub fn on_keydown(vk: VIRTUAL_KEY, mods: Mods) -> Option<Hit> {
    let toggle = match shell::toggle_combo() {
        Some(combo) => rules::matches_combo(vk, mods, &combo),
        None => rules::is_toggle(vk, mods, shell::toggle_hotkey()),
    };
    if toggle {
        return Some(Hit::Toggle);
    }
    let flip = match shell::flip_combo() {
        Some(combo) => rules::matches_combo(vk, mods, &combo),
        None => rules::is_flip(vk, mods, shell::flip_hotkey()),
    };
    flip.then_some(Hit::Flip)
}

/// Feed every key event to the modifier-only gesture tracker, including the ones
/// the engine goes on to swallow — a swallowed keystroke still means the user
/// was typing a shortcut, not tapping a bare modifier pair.
pub fn on_key_event(vk: VIRTUAL_KEY, mods: Mods, down: bool) -> Option<Hit> {
    let event = match (rules::is_modifier(vk), down) {
        (true, true) => Event::ModifierDown,
        (true, false) => Event::ModifierUp,
        (false, true) => Event::OtherDown,
        (false, false) => return None, // an ordinary key coming up changes nothing
    };
    let held = rules::mods_with(vk, mods, down);
    let gesture = WATCHER.with(|w| w.borrow_mut().feed(event, held))?;

    if toggle_mods() == Some(gesture) {
        return Some(Hit::Toggle);
    }
    (flip_mods() == Some(gesture)).then_some(Hit::Flip)
}

/// Whether this virtual-key is a modifier (either side, generic or specific).
pub fn is_modifier_key(vk: VIRTUAL_KEY) -> bool {
    rules::is_modifier(vk)
}

/// Abandon the gesture in progress. Clicking or scrolling with modifiers down is
/// a Ctrl+click or a Ctrl+wheel zoom, never a bare modifier tap.
pub fn note_other_input() {
    WATCHER.with(|w| w.borrow_mut().interrupt());
}

/// The modifier-only set bound to the toggle, if it is bound to one at all.
fn toggle_mods() -> Option<Mods> {
    match shell::toggle_combo() {
        Some(combo) => rules::combo_mods(&combo),
        None => rules::preset_mods(shell::toggle_hotkey()),
    }
}

fn flip_mods() -> Option<Mods> {
    // No flip preset is modifier-only, so only a recorded combo can be one.
    rules::combo_mods(&shell::flip_combo()?)
}
