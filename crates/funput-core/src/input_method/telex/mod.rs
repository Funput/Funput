//! Telex key classification. Modifier-specific classifiers stay isolated so the
//! common dispatch remains small and its priority is explicit.

mod advanced;
mod modifiers;

use crate::input_method::{AdvancedAction, KeyAction};

pub use modifiers::tone::tone_from_key;

pub fn classify_advanced_key(buffer: &str, key: char) -> AdvancedAction {
    advanced::classify(buffer, key)
}

/// Classify a Telex keystroke into a method-agnostic action.
pub fn classify_key(buffer: &str, key: char) -> KeyAction {
    if crate::composition::intent::has_pending(buffer) {
        return KeyAction::DeferredW;
    }
    if let Some(action) = modifiers::stroke::classify(buffer, key) {
        return action;
    }
    if let Some(action) = modifiers::circumflex::classify(buffer, key) {
        return action;
    }
    if key.eq_ignore_ascii_case(&'w')
        && let Some(action) = modifiers::w::classify(buffer)
    {
        return action;
    }
    if key.eq_ignore_ascii_case(&'z') {
        return KeyAction::RemoveTone;
    }
    modifiers::tone::classify(buffer, key).unwrap_or(KeyAction::Normal)
}

pub(crate) fn classify_w(buffer: &str) -> Option<KeyAction> {
    modifiers::w::classify(buffer)
}

pub(super) fn last_char(buffer: &str) -> Option<char> {
    buffer.chars().last()
}

#[cfg(test)]
mod tests;
