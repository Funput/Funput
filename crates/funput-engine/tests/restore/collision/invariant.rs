//! What collision restore can never do: change anything but the boundary of a word
//! whose keys are listed.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules};
use proptest::prelude::*;

use super::{DEFERRED_W, engine};

const METHODS: [InputMethod; 3] = [
    InputMethod::Telex,
    InputMethod::TelexAdvanced,
    InputMethod::Vni,
];

/// Listed words and their near misses, glued to letters, tone keys, digits,
/// Backspace (`<`), Flip (`^`) and boundaries, so a listed word meets all of them.
const TOKENS: [&str; 24] = [
    "two", "swan", "sworn", "tw", "sw", "c", "t", "s", "w", "o", "a", "u", "n", "m", "g", "r", "f",
    "j", "2", "T", "<", "^", " ", ".",
];

fn script() -> impl Strategy<Value = String> {
    proptest::collection::vec(proptest::sample::select(&TOKENS[..]), 1..16)
        .prop_map(|tokens| tokens.concat())
}

/// Whether the live word's keys are listed: digits glued to the end do not count.
fn listed(keys: &str) -> bool {
    let word = keys.trim_end_matches(|key: char| key.is_ascii_digit());
    DEFERRED_W
        .lines()
        .any(|entry| entry.eq_ignore_ascii_case(word))
}

proptest! {
    /// Every step gets the same instruction with the feature on as with it off,
    /// except a boundary that ends a listed word in plain Telex.
    #[test]
    fn only_a_listed_word_ending_changes(
        script in script(),
        method in proptest::sample::select(&METHODS[..]),
        eager in any::<bool>(),
        wide in any::<bool>(),
    ) {
        let configured = |restore_collisions: bool| {
            let mut engine = engine(method, restore_collisions);
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
        for (index, step) in script.chars().enumerate() {
            let exempt = matches!(step, ' ' | '.')
                && method == InputMethod::Telex
                && listed(off.keys());
            let (expected, actual) = match step {
                '<' => (off.on_backspace(), on.on_backspace()),
                '^' => (off.flip_composing(), on.flip_composing()),
                key => (off.process_char(key), on.process_char(key)),
            };
            if !exempt {
                prop_assert_eq!(&actual, &expected, "{:?} at step {}", script, index);
            }
        }
    }
}
