//! Property-based tests for Telex composition. Random keystroke sequences must never
//! panic and must satisfy structural invariants for *any* input — complementing the
//! fixed fixture/corpus cases.

use funput_core::InputMethod;
use proptest::prelude::*;

use crate::support::type_keys;

fn telex(keys: &str) -> String {
    type_keys(InputMethod::Telex, keys)
}

fn advanced(keys: &str) -> String {
    type_keys(InputMethod::TelexAdvanced, keys)
}

proptest! {
    /// Any run of letters/digits composes without panicking, and composition never
    /// produces more characters than were typed — diacritics/shapes only *combine*
    /// keystrokes, they never add characters.
    #[test]
    fn never_panics_and_never_grows(keys in "[a-zA-Z0-9]{0,24}") {
        let out = telex(&keys);
        prop_assert!(
            out.chars().count() <= keys.chars().count(),
            "composition grew: {keys:?} -> {out:?}"
        );
    }

    /// Composition is deterministic — no hidden global state between runs.
    #[test]
    fn deterministic(keys in "[a-zA-Z0-9]{0,24}") {
        prop_assert_eq!(telex(&keys), telex(&keys));
    }

    /// Full Telex adds punctuation-like shortcut keys, but retains the same safety
    /// and determinism invariants as the standard classifier.
    #[test]
    fn advanced_never_panics_or_grows(keys in "[a-zA-Z0-9\\[\\]{}]{0,24}") {
        let output = advanced(&keys);
        prop_assert!(output.chars().count() <= keys.chars().count());
        prop_assert_eq!(&output, &advanced(&keys));
    }

    /// Braces remain outside Full Telex and must survive arbitrary literal runs.
    #[test]
    fn advanced_braces_are_literal(keys in "[{}]{0,16}") {
        prop_assert_eq!(advanced(&keys), keys);
    }

    /// A run of consonants that are not Telex modifiers (no tone `s/f/r/x/j`, no shape
    /// `w`, no stroke `d`, no vowels) passes through unchanged — nothing should ever
    /// sprout a diacritic on its own.
    #[test]
    fn plain_consonants_pass_through(keys in "[bcghklmnpqtvBCGHKLMNPQTV]{0,16}") {
        prop_assert_eq!(telex(&keys), keys);
    }
}
