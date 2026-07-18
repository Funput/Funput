use crate::composition::intent::has_pending;
use crate::input_method::{AdvancedAction, KeyAction, TelexShortcut};
use crate::unicode::marks::is_vowel;

use super::classify_key;

/// Full Telex classifier. Ordinary Telex keeps its unchanged fast path.
pub(super) fn classify(buffer: &str, key: char) -> AdvancedAction {
    if has_pending(buffer) {
        return AdvancedAction::Standard(KeyAction::DeferredW);
    }
    match key {
        '[' => AdvancedAction::Shortcut(TelexShortcut::HornU),
        ']' => AdvancedAction::Shortcut(TelexShortcut::HornO),
        'w' | 'W' if buffer.is_empty() || standalone_horn_u(buffer) => {
            AdvancedAction::Shortcut(TelexShortcut::LeadingW)
        }
        'w' | 'W' if ends_with_w_after_vowel(buffer) => {
            AdvancedAction::Shortcut(TelexShortcut::RepeatedW)
        }
        _ => AdvancedAction::Standard(classify_key(buffer, key)),
    }
}

fn ends_with_w_after_vowel(buffer: &str) -> bool {
    buffer
        .chars()
        .last()
        .is_some_and(|ch| ch.eq_ignore_ascii_case(&'w'))
        && buffer.chars().any(is_vowel)
}

fn standalone_horn_u(buffer: &str) -> bool {
    matches!(buffer, "ư" | "Ư")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn recognizes_only_full_telex_extensions() {
        assert_eq!(
            classify("", 'w'),
            AdvancedAction::Shortcut(TelexShortcut::LeadingW)
        );
        assert_eq!(
            classify("t", '['),
            AdvancedAction::Shortcut(TelexShortcut::HornU)
        );
        assert_eq!(
            classify("m", ']'),
            AdvancedAction::Shortcut(TelexShortcut::HornO)
        );
        assert_eq!(
            classify("lw", '['),
            AdvancedAction::Standard(KeyAction::DeferredW)
        );
        assert_eq!(
            classify("a", 's'),
            AdvancedAction::Standard(classify_key("a", 's'))
        );
    }
}
