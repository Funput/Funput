//! What doubled-key restore can never do: change a word that had no revert.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules};
use funput_engine::Engine;
use proptest::prelude::*;

use super::engine;

const METHODS: [InputMethod; 3] = [
    InputMethod::Telex,
    InputMethod::TelexAdvanced,
    InputMethod::Vni,
];

/// What a script is built from: letters weighted toward tone keys and their doubles,
/// so reverts, boundaries, Backspace (`<`) and Flip (`^`) meet often.
const TOKENS: [&str; 30] = [
    "a", "e", "o", "u", "i", "y", "p", "t", "c", "l", "n", "g", "h", "d", "w", "1", "s", "f", "r",
    "x", "j", "S", "A", "ss", "ff", "rr", "<", "<<<", "^", " ",
];

fn script() -> impl Strategy<Value = String> {
    proptest::collection::vec(proptest::sample::select(&TOKENS[..]), 1..24)
        .prop_map(|tokens| tokens.concat())
}

proptest! {
    /// Until a revert swallows a key, every step — Backspace (`<`), Flip (`^`) and
    /// the boundary included — gets the same instruction with the feature on as with
    /// it off, with eager restore on or off and with or without the extra onsets. A
    /// boundary, or Backspace down to nothing, ends the word, so what follows has to
    /// agree again from its first key: a revert must not leak past its own word. VNI never reverts on a letter, so
    /// there the two agree on every step whatever happens.
    #[test]
    fn a_word_without_a_revert_types_exactly_as_before(
        script in script(),
        method in proptest::sample::select(&METHODS[..]),
        eager in any::<bool>(),
        wide in any::<bool>(),
    ) {
        let configured = |restore_doubled_keys: bool| {
            let mut engine = engine(method, restore_doubled_keys);
            engine.update_config(|config| {
                config.eager_restore = eager;
                if wide {
                    config.syllable_rules =
                        SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
                }
            });
            engine
        };
        let (mut on, mut off) = (configured(true), configured(false));
        let mut reverted = false;
        for (index, step) in script.chars().enumerate() {
            let (expected, actual) = match step {
                '<' => (off.on_backspace(), on.on_backspace()),
                '^' => (off.flip_composing(), on.flip_composing()),
                key => {
                    let appended = format!("{}{key}", off.keys());
                    let pair = (off.process_char(key), on.process_char(key));
                    // With the feature off, a revert is the only key that rewrites a
                    // live word's keys instead of appending to them. Only one on keys
                    // the engine saw typed can start a literal: keys Backspace seeded
                    // from the screen (`â` + `a`) must still type exactly as before.
                    reverted |= !off.keys().is_empty()
                        && off.keys() != appended
                        && appended.is_ascii();
                    pair
                }
            };
            if !reverted || method == InputMethod::Vni {
                prop_assert_eq!(&actual, &expected, "{:?} at step {}", script, index);
            }
            // The word ends at a boundary, or when Backspace has emptied it on both sides.
            let emptied = step == '<' && on.buffer().is_empty() && off.buffer().is_empty();
            if step == ' ' || emptied {
                reverted = false;
            }
        }
    }
}

/// The case the property exists for, spelled out: a revert in one word, then a word
/// typed after it, which must come out as if the first had never been typed.
#[test]
fn a_revert_does_not_reach_the_next_word() {
    let type_in = |engine: &mut Engine, keys: &str| {
        keys.chars()
            .map(|key| engine.process_char(key))
            .collect::<Vec<_>>()
    };
    let mut on = engine(InputMethod::Telex, true);
    let mut off = engine(InputMethod::Telex, false);
    type_in(&mut on, "pass ");
    type_in(&mut off, "pass ");
    assert_eq!(type_in(&mut on, "chao"), type_in(&mut off, "chao"));
    // Flip reads the keys as typed, so a literal left over would show here.
    assert_eq!(on.flip_composing(), off.flip_composing());
    assert_eq!(type_in(&mut on, "f "), type_in(&mut off, "f "));
}

/// The same through Backspace: a one-letter word with a literal (`ww` in Telex nâng
/// cao), erased whole, must leave nothing behind for Flip to show. The property
/// reaches this case too rarely to count on it.
#[test]
fn backspace_over_a_reverted_word_leaves_nothing_behind() {
    let mut on = engine(InputMethod::TelexAdvanced, true);
    let mut off = engine(InputMethod::TelexAdvanced, false);
    for engine in [&mut on, &mut off] {
        engine.process_char('w');
        engine.process_char('w');
        engine.on_backspace();
        engine.process_char('a');
    }
    assert_eq!(on.flip_composing(), off.flip_composing());
}
