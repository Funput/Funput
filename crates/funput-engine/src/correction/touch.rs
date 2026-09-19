//! Where the fingers landed — the input the platform feeds correction with.
//!
//! [`KeyTouch`] is the public shape a host reports per keystroke: the key it resolved
//! the touch to, and the neighbours the same touch could have meant. Distances are in
//! key pitches — the gap between two neighbouring key centres — so the model is
//! independent of screen size and keyboard height.
//!
//! # Layout
//!
//! - this file — the public [`KeyTouch`], quantisation, and the four points where
//!   the engine feeds or checks the log.
//! - `log` — the per-word record those points maintain.

mod log;

pub(crate) use log::{Alternate, MAX_WORD_KEYS, StoredTouch, TouchLog};

use crate::model::Session;

/// Neighbours a host may offer per touch.
pub const MAX_ALTERNATES: usize = 3;

/// One keystroke's touch evidence, as the platform measures it.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct KeyTouch {
    typed: char,
    typed_distance: f32,
    alternates: [(char, f32); MAX_ALTERNATES],
    count: u8,
}

impl KeyTouch {
    /// A touch that landed `distance` key-pitches from the centre of `typed`.
    pub fn new(typed: char, distance: f32) -> Self {
        Self {
            typed,
            typed_distance: distance,
            alternates: [('\0', 0.0); MAX_ALTERNATES],
            count: 0,
        }
    }

    /// Add a neighbouring key the same touch could have meant. Offers past
    /// [`MAX_ALTERNATES`] are dropped, so hosts should add the nearest first.
    #[must_use]
    pub fn with_alternate(mut self, key: char, distance: f32) -> Self {
        let slot = usize::from(self.count);
        if slot < MAX_ALTERNATES {
            self.alternates[slot] = (key, distance);
            self.count += 1;
        }
        self
    }

    fn store(&self) -> StoredTouch {
        let mut stored = StoredTouch {
            typed: self.typed,
            typed_distance: to_q8(self.typed_distance),
            count: self.count,
            ..StoredTouch::default()
        };
        for (slot, &(key, distance)) in stored.alternates.iter_mut().zip(&self.alternates) {
            *slot = Alternate {
                key,
                distance: to_q8(distance),
            };
        }
        stored
    }
}

/// Q8.8: 1/256 of a key pitch, saturating at 255.99 — far past any distance that
/// could still name a plausible key.
fn to_q8(distance: f32) -> u16 {
    (distance * 256.0).round().clamp(0.0, f32::from(u16::MAX)) as u16
}

pub(crate) fn from_q8(distance: u16) -> f32 {
    f32::from(distance) / 256.0
}

/// Hold `touch` for the key the host is about to send.
pub(crate) fn set_next(session: &mut Session, touch: KeyTouch) {
    if let Some(state) = session.correction.as_mut() {
        state.next_touch = Some(touch.store());
    }
}

/// Take the touch recorded for the key now being processed. Called once per key
/// whatever the key turns out to be, so a touch never leaks onto a later keystroke.
pub(crate) fn take_next(session: &mut Session) -> Option<StoredTouch> {
    session.correction.as_mut()?.next_touch.take()
}

/// Record the touch behind a key that just joined the raw keys. A key with no touch
/// data leaves the word uncorrectable rather than half-described.
pub(crate) fn note_key(session: &mut Session, raw_key: char, touch: Option<StoredTouch>) {
    let Some(state) = session.correction.as_mut() else {
        return;
    };
    match touch {
        Some(touch) => state.touch.push(raw_key, touch),
        None => state.touch.invalidate(),
    }
}

/// Give up on correcting the live word — something rewrote the raw keys, so the log
/// no longer describes what the user typed.
pub(crate) fn invalidate(session: &mut Session) {
    if let Some(state) = session.correction.as_mut() {
        state.touch.invalidate();
    }
}

/// Catch the raw keys being rewritten under the log — a reverted modifier, an adopted
/// word, a digit the engine swallowed.
pub(crate) fn verify_alignment(session: &mut Session) {
    let keys = session.keys.chars().count();
    if let Some(state) = session.correction.as_mut()
        && state.touch.len() != keys
    {
        state.touch.invalidate();
    }
}

#[cfg(test)]
mod tests;
