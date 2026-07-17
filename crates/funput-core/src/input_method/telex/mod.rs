//! Telex key classification. Modifier-specific classifiers stay isolated so the
//! common dispatch remains small and its priority is explicit.

mod circumflex;
mod stroke;
mod tone;
mod w;

use crate::input_method::KeyAction;

pub use tone::tone_from_key;

/// Classify a Telex keystroke into a method-agnostic action.
pub fn classify_key(buffer: &str, key: char) -> KeyAction {
    if crate::composition::intent::has_pending(buffer) {
        return KeyAction::Normal;
    }
    if let Some(action) = stroke::classify(buffer, key) {
        return action;
    }
    if let Some(action) = circumflex::classify(buffer, key) {
        return action;
    }
    if key.eq_ignore_ascii_case(&'w')
        && let Some(action) = w::classify(buffer)
    {
        return action;
    }
    if key.eq_ignore_ascii_case(&'z') {
        return KeyAction::RemoveTone;
    }
    tone::classify(buffer, key).unwrap_or(KeyAction::Normal)
}

pub(crate) fn classify_w(buffer: &str) -> Option<KeyAction> {
    w::classify(buffer)
}

pub(super) fn last_char(buffer: &str) -> Option<char> {
    buffer.chars().last()
}

#[cfg(test)]
mod tests;
