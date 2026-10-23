//! The hook thread's [`AccessKeys`]: whether the keys being typed belong to the
//! focused app's menus — Office's KeyTips after a lone Alt tap — and must reach it
//! without being composed.
//!
//! Fed from all three hooks, since each sees a different way out of the menus:
//! [`super::keyboard`] reports the Alt tap and asks about every key, [`super::mouse`]
//! a click, and [`super::foreground`] a switch to another window.

use std::cell::RefCell;
use std::time::Instant;

use funput_desktop::{AccessKeys, Caret, KeyKind, Mods};

use crate::shared::shell;

thread_local! {
    /// The hooks all run on the background thread's message pump, so the mode
    /// lives with that thread instead of behind a lock — as the hotkey tracker does.
    static STATE: RefCell<AccessKeys> = RefCell::new(AccessKeys::default());
}

/// A bare-modifier gesture completed that is not one of Funput's hotkeys.
pub(super) fn on_modifier_tap(tapped: Mods) {
    let entered = STATE.with(|s| s.borrow_mut().on_modifier_tap(tapped, Instant::now()));
    if entered {
        // The app's menus have the keyboard now; the word being composed is
        // finished as it stands, and the caret is wherever the menu leaves it.
        shell::caret_moved(Caret::Unknown);
    }
}

/// Whether this key must reach the app as typed, skipping the engine.
pub(super) fn bypasses(kind: &KeyKind) -> bool {
    STATE.with(|s| s.borrow_mut().bypasses(kind, Instant::now()))
}

/// The user is back in the document: a click, another window, or a hotkey.
pub(super) fn cancel() {
    STATE.with(|s| s.borrow_mut().cancel());
}
