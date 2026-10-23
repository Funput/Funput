//! Access-key mode: the keys after a bare Alt tap belong to the app's menus.
//!
//! Tapping Alt on its own (pressed and released with nothing else) hands the
//! keyboard to the menu bar or, in Office, the ribbon's KeyTips. The letters that
//! follow are accelerators, typed one at a time with Alt already up — Excel's
//! Alt, E, S, V is Paste Special → Values. A hook shell only knows a shortcut by
//! the modifier still being held, so without this it composes them as text:
//! Telex turns `e` + `s` into `é`, the app gets a Backspace and an `é` instead of
//! the `S`, and the sequence dies halfway.
//!
//! [`AccessKeys`] remembers that the mode was entered, and says which keys must
//! reach the app untouched until it ends. The host cannot see when the app leaves
//! the mode, so every way out here is a guess made on the safe side: the keys that
//! end a menu (Enter, Esc, arrows, any shortcut — [`KeyKind::Flush`]), the user
//! moving on elsewhere (a click, another window — [`AccessKeys::cancel`]), a space,
//! and a pause of [`IDLE_TIMEOUT`]. Should the app have left the mode without the
//! host noticing, the cost is bounded to the first word typed afterwards.
//!
//! Pure and host-neutral: the shell reports gestures and keys and passes the clock
//! in, so the rules here are unit tested without any OS. Further ways *into* the
//! mode (F10, the Menu key) would each be one more `on_*` entry point.

use std::time::{Duration, Instant};

use crate::{KeyKind, Mods};

/// How long the mode survives with no key typed in it.
///
/// Only a safety net for an Alt tap that opened nothing — the common exits are the
/// keys above. Long enough to read a KeyTip before pressing it, short enough that
/// someone who tapped Alt by accident is composing again by their next sentence.
pub const IDLE_TIMEOUT: Duration = Duration::from_secs(3);

/// Whether the keys being typed are going to an app's menus rather than its text.
#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
pub struct AccessKeys {
    /// When the mode last saw activity, while it is on.
    since: Option<Instant>,
}

impl AccessKeys {
    /// A bare-modifier gesture completed: `tapped` was pressed and released with no
    /// other key or click in between. Only Alt alone opens the menus, and a second
    /// tap closes them again — the same toggle Windows applies.
    ///
    /// Returns true when this *entered* the mode, so the shell can commit the word
    /// being composed: the caret's owner no longer has the keyboard.
    pub fn on_modifier_tap(&mut self, tapped: Mods, now: Instant) -> bool {
        if tapped != ALT_ALONE {
            return false;
        }
        if self.is_active(now) {
            self.cancel();
            return false;
        }
        self.since = Some(now);
        true
    }

    /// Whether this key must bypass composition and reach the app as typed.
    ///
    /// Takes the [`KeyKind`] the shell already classified, so a key is judged once.
    /// False means "handle it as usual" — including the key that ends the mode
    /// when it is one the shell must still act on, such as Enter's flush.
    pub fn bypasses(&mut self, kind: &KeyKind, now: Instant) -> bool {
        if !self.is_active(now) {
            self.cancel();
            return false;
        }
        match kind {
            // A space has no meaning to a menu, and is the surest sign the user is
            // typing text again: let it through as typed, and compose from here on.
            KeyKind::Compose(c, _) if c.is_whitespace() => {
                self.cancel();
                true
            }
            // Accelerators — and Backspace, which steps a KeyTip back one level.
            KeyKind::Compose(..) | KeyKind::Backspace | KeyKind::PassThrough => {
                self.since = Some(now);
                true
            }
            // Enter, Esc, arrows or a real shortcut: the menu is done with, and the
            // shell's own flush still has to run for it.
            KeyKind::Flush(_) => {
                self.cancel();
                false
            }
        }
    }

    /// Leave the mode: the user clicked, switched windows, or ran a hotkey.
    pub fn cancel(&mut self) {
        self.since = None;
    }

    /// Whether the mode is on at `now`, the idle timeout included.
    pub fn is_active(&self, now: Instant) -> bool {
        self.since
            .is_some_and(|since| now.saturating_duration_since(since) < IDLE_TIMEOUT)
    }
}

/// The one bare-modifier gesture that opens an app's menus.
const ALT_ALONE: Mods = Mods {
    ctrl: false,
    alt: true,
    win: false,
    shift: false,
};

#[cfg(test)]
mod tests;
