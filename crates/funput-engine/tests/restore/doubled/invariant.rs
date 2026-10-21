//! What doubled-key restore can never do: change a word that had no revert.

use funput_core::InputMethod;
use proptest::prelude::*;

use super::engine;

const METHODS: [InputMethod; 3] = [
    InputMethod::Telex,
    InputMethod::TelexAdvanced,
    InputMethod::Vni,
];

proptest! {
    /// Until a revert swallows a key, every keystroke — the boundary included — gets
    /// the same instruction with the feature on as with it off. VNI never reverts on
    /// a letter, so there the two agree on every keystroke whatever happens.
    #[test]
    fn a_word_without_a_revert_types_exactly_as_before(
        keys in "[a-z0-9]{1,10}",
        method in proptest::sample::select(&METHODS[..]),
    ) {
        let mut on = engine(method, true);
        let mut off = engine(method, false);
        let mut reverted = false;
        for (index, key) in keys.chars().chain([' ']).enumerate() {
            let before = off.keys().chars().count();
            let expected = off.process_char(key);
            let actual = on.process_char(key);
            // A revert is the only keystroke that leaves a live word's keys no longer.
            let after = off.keys().chars().count();
            reverted |= after > 0 && after <= before;
            if !reverted || method == InputMethod::Vni {
                prop_assert_eq!(&actual, &expected, "{:?} at key {}", keys, index);
            }
        }
    }
}
