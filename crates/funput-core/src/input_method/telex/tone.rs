use crate::input_method::KeyAction;
use crate::unicode::marks::{Tone, is_vowel};

/// Map Telex tone keys to tone marks.
pub fn tone_from_key(key: char) -> Option<Tone> {
    match key.to_ascii_lowercase() {
        's' => Some(Tone::Sac),
        'f' => Some(Tone::Huyen),
        'r' => Some(Tone::Hoi),
        'x' => Some(Tone::Nga),
        'j' => Some(Tone::Nang),
        _ => None,
    }
}

pub(super) fn classify(buffer: &str, key: char) -> Option<KeyAction> {
    buffer
        .chars()
        .any(is_vowel)
        .then(|| tone_from_key(key))
        .flatten()
        .map(KeyAction::Tone)
}
