//! Whether the engine leaves an English word Vietnamese, found by typing it.
//!
//! Asking the real engine, as `doubled` does, keeps the lists in step with the core:
//! a change that stops a rule from shaping a word, or lets the boundary restore it
//! anyway, drops the word from the list the next time it is generated.

use funput_engine::Action;

use crate::wordlist::setup::Setup;

/// Whether `word` typed under `setup`, then a space, stays a Vietnamese composition
/// with collision restore off — the user-visible bug the list exists to fix.
///
/// Three things must hold: no revert rewrote the keys (the word was typed as is),
/// the screen differs from the keys (something composed), and the boundary kept
/// that composition (English restore did not already give the word back).
pub(crate) fn left_vietnamese(word: &str, setup: Setup) -> bool {
    let mut engine = setup.engine(|config| config.restore_collisions = false);
    for key in word.chars() {
        engine.process_char(key);
    }
    let composed = engine.keys() == word && engine.buffer() != word;
    composed && engine.process_char(' ').action == Action::None
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::wordlist::setup::SETUPS;

    const TELEX: Setup = SETUPS[0];

    #[test]
    fn a_word_composed_into_a_syllable_is_left_vietnamese() {
        for word in ["two", "swan", "how"] {
            assert!(left_vietnamese(word, TELEX), "{word}");
        }
    }

    #[test]
    fn a_word_typed_as_is_or_restored_is_not() {
        // Nothing composes, the boundary restores a non-syllable, or eager restore
        // already showed the keys.
        for word in ["the", "text", "swift", "twin"] {
            assert!(!left_vietnamese(word, TELEX), "{word}");
        }
    }
}
