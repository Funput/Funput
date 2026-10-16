//! The replay budget: no word, whatever its keys and whatever the host reported,
//! replays more than the cap — and the common slip is settled well under it.

use funput_core::InputMethod;
use proptest::prelude::*;

use super::{engine, type_word};
use crate::correction::generate::MAX_REPLAYS_PER_WORD;
use crate::{Engine, KeyTouch};

fn replays_after(engine: &mut Engine) -> u32 {
    engine.process_char(' ');
    engine.correction_replays_last()
}

proptest! {
    #[test]
    fn a_word_with_touches_never_overruns_the_budget(keys in "[a-z]{2,12}") {
        let mut engine = engine(InputMethod::Telex);
        type_word(&mut engine, &keys, &[(0, 's'), (1, 'g')]);
        prop_assert!(replays_after(&mut engine) <= MAX_REPLAYS_PER_WORD);
    }

    #[test]
    fn a_word_without_touches_never_overruns_the_budget(keys in "[a-z]{2,12}") {
        let mut engine = engine(InputMethod::Telex);
        for key in keys.chars() {
            engine.process_char(key);
        }
        prop_assert!(replays_after(&mut engine) <= MAX_REPLAYS_PER_WORD);
    }
}

#[test]
fn a_bounced_key_is_settled_in_the_cheap_tiers() {
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "chowii", &[]);
    let replays = replays_after(&mut engine);
    assert!(replays < 120, "{replays} replays");
    assert_eq!(engine.correction_candidates()[0].text(), "chơi");
}

/// A slip the finger reported costs about a nat: plus the margin, that is under
/// the last tier's floor, so the search stops after the cheap pairs and never tries
/// an insertion for it. The cap is for the word every tier fails.
#[test]
fn a_neighbour_slip_is_settled_before_the_dear_tiers() {
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "dduwowfnh", &[(8, 'g')]);
    let replays = replays_after(&mut engine);
    assert!(replays < 120, "{replays} replays");
    assert_eq!(engine.correction_candidates()[0].text(), "đường");
}

#[test]
fn a_long_word_no_edit_rescues_spends_the_budget_and_stops() {
    let mut engine = engine(InputMethod::Telex);
    for key in "bcdfghjklmnp".chars() {
        engine.set_next_key_touch(
            KeyTouch::new(key, 0.3)
                .with_alternate('o', 0.6)
                .with_alternate('a', 0.7)
                .with_alternate('e', 0.8),
        );
        engine.process_char(key);
    }
    assert_eq!(replays_after(&mut engine), MAX_REPLAYS_PER_WORD);
    assert!(!engine.has_pending_correction());
}
