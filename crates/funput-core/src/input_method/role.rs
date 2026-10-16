//! The role a key plays in a method, read off the key alone.
//!
//! [`key_role`] answers "is this key a letter or a modifier?" without looking at
//! the syllable buffer, which is the question a *typo* model asks: a modifier
//! that lands where none belongs (`chowii`, `cho7ii`) is a cheap, likely slip,
//! and the model has to know which keys those are for each method without
//! carrying its own copy of the tables.
//!
//! Context-free on purpose. The context-sensitive Telex keys — `a`, `e`, `o`
//! and `d`, which become a circumflex or a stroke only when doubled — are
//! [`KeyRole::Letter`]: a caller that cares about the pair looks at the pair.
//! The answer is therefore not what [`crate::apply`] will do with the key; it is
//! what the key *could* mean, and it never changes how the pipeline composes.

use crate::input_method::{telex, vni};
use crate::options::InputMethod;

/// What a key can mean in a method, independent of what precedes it.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum KeyRole {
    /// An ordinary letter, including the Telex vowels that only shape when doubled.
    Letter,
    /// A tone mark (Telex `s f r x j`, VNI `1`–`5`).
    Tone,
    /// A vowel shape (Telex `w`, Advanced `[` `]`, VNI `6`–`8`).
    Shape,
    /// The `đ` stroke, where a method has a dedicated key for it (VNI `9`).
    Stroke,
    /// Remove the tone mark (Telex `z`, VNI `0`).
    Remove,
    /// Anything else: digits in Telex, punctuation, space.
    Other,
}

/// The role `key` plays in `method`, case-insensitively and with no buffer in
/// sight. See the module doc for what "context-free" leaves out.
pub fn key_role(method: InputMethod, key: char) -> KeyRole {
    match method {
        InputMethod::Vni => vni_role(key),
        InputMethod::Telex => telex_role(key),
        InputMethod::TelexAdvanced => match key {
            '[' | ']' => KeyRole::Shape,
            _ => telex_role(key),
        },
    }
}

fn vni_role(key: char) -> KeyRole {
    match key {
        '9' => KeyRole::Stroke,
        '0' => KeyRole::Remove,
        _ if vni::tone_from_digit(key).is_some() => KeyRole::Tone,
        _ if vni::shape_from_digit(key).is_some() => KeyRole::Shape,
        _ if key.is_ascii_alphabetic() => KeyRole::Letter,
        _ => KeyRole::Other,
    }
}

fn telex_role(key: char) -> KeyRole {
    match key.to_ascii_lowercase() {
        'w' => KeyRole::Shape,
        'z' => KeyRole::Remove,
        _ if telex::tone_from_key(key).is_some() => KeyRole::Tone,
        _ if key.is_ascii_alphabetic() => KeyRole::Letter,
        _ => KeyRole::Other,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use InputMethod::{Telex, TelexAdvanced, Vni};
    use KeyRole::*;

    #[test]
    fn telex_modifiers_by_role() {
        for key in ['s', 'f', 'r', 'x', 'j'] {
            assert_eq!(key_role(Telex, key), Tone, "{key}");
        }
        assert_eq!(key_role(Telex, 'w'), Shape);
        assert_eq!(key_role(Telex, 'z'), Remove);
    }

    /// The doubled vowels and `dd` need a buffer to mean anything; alone they are
    /// letters, and the caller that cares about the pair looks at the pair.
    #[test]
    fn telex_context_keys_are_letters() {
        for key in ['a', 'e', 'o', 'd'] {
            assert_eq!(key_role(Telex, key), Letter, "{key}");
            assert_eq!(key_role(TelexAdvanced, key), Letter, "{key}");
        }
        assert_eq!(key_role(Telex, 'n'), Letter);
    }

    #[test]
    fn brackets_shape_only_in_advanced_telex() {
        assert_eq!(key_role(TelexAdvanced, '['), Shape);
        assert_eq!(key_role(TelexAdvanced, ']'), Shape);
        assert_eq!(key_role(Telex, '['), Other);
        assert_eq!(key_role(Telex, ']'), Other);
        assert_eq!(key_role(Vni, '['), Other);
    }

    #[test]
    fn vni_digits_by_role() {
        for key in ['1', '2', '3', '4', '5'] {
            assert_eq!(key_role(Vni, key), Tone, "{key}");
        }
        for key in ['6', '7', '8'] {
            assert_eq!(key_role(Vni, key), Shape, "{key}");
        }
        assert_eq!(key_role(Vni, '9'), Stroke);
        assert_eq!(key_role(Vni, '0'), Remove);
        assert_eq!(key_role(Vni, 's'), Letter, "VNI letters never modify");
        assert_eq!(key_role(Telex, '1'), Other, "Telex digits never modify");
    }

    #[test]
    fn case_does_not_matter() {
        assert_eq!(key_role(Telex, 'S'), Tone);
        assert_eq!(key_role(Telex, 'W'), Shape);
        assert_eq!(key_role(Telex, 'Z'), Remove);
        assert_eq!(key_role(Telex, 'A'), Letter);
        assert_eq!(key_role(Vni, 'A'), Letter);
    }

    #[test]
    fn punctuation_and_space_are_other() {
        for method in [Telex, TelexAdvanced, Vni] {
            for key in [' ', '.', ',', '-', '\'', 'đ'] {
                assert_eq!(key_role(method, key), Other, "{method:?} {key:?}");
            }
        }
    }
}
