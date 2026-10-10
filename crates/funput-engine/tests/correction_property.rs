//! Invariants typo correction must hold for any input at all.
//!
//! The fixed cases say what correction *does*; these say what it can never do. The
//! first one is the backbone of the whole feature: a word that is already a real
//! Vietnamese syllable is what the user meant, and nothing may rewrite it.

mod support;

use funput_core::InputMethod;
use funput_engine::{Action, Engine};
use proptest::prelude::*;

use support::{Document, candidate_texts, correcting_engine, type_touched};

/// The QWERTY neighbour to the right of each letter — a plausible slip for any key.
const ROWS: [&str; 3] = ["qwertyuiop", "asdfghjkl", "zxcvbnm"];

fn neighbour(key: char) -> Option<char> {
    ROWS.iter().find_map(|row| {
        let index = row.find(key)?;
        row[index + 1..].chars().next()
    })
}

/// Type `keys` with a neighbour offered for every key, and return the engine, the
/// document, and the word as it stood just before the space.
fn typed(method: InputMethod, keys: &str) -> (Engine, Document, String) {
    let mut engine = correcting_engine(method);
    let mut doc = Document::new();
    let slips: Vec<(usize, char)> = keys
        .chars()
        .enumerate()
        .filter_map(|(i, key)| neighbour(key).map(|alternate| (i, alternate)))
        .collect();
    type_touched(&mut engine, &mut doc, keys, &slips);
    let shown = engine.buffer().to_owned();
    type_touched(&mut engine, &mut doc, " ", &[]);
    (engine, doc, shown)
}

proptest! {
    /// A word that composed into a finished syllable is what the user meant.
    #[test]
    fn a_complete_syllable_is_never_offered_a_correction(keys in "[a-z]{1,10}") {
        let (engine, _, shown) = typed(InputMethod::Telex, &keys);
        if funput_core::is_complete_syllable(&shown) {
            prop_assert!(
                !engine.has_pending_correction(),
                "{keys:?} shows {shown:?} and was offered {:?}",
                candidate_texts(&engine)
            );
        }
    }

    /// Offering the word that is already on screen would be a correction that
    /// corrects nothing.
    #[test]
    fn no_candidate_ever_equals_the_word_on_screen(keys in "[a-z]{1,10}") {
        let (engine, _, shown) = typed(InputMethod::Telex, &keys);
        prop_assert!(!candidate_texts(&engine).contains(&shown.as_str()));
    }

    #[test]
    fn candidates_always_come_back_in_descending_order(keys in "[a-z]{1,10}") {
        let (engine, _, _) = typed(InputMethod::Telex, &keys);
        let scores: Vec<f32> = engine
            .correction_candidates()
            .iter()
            .map(funput_engine::CorrectionCandidate::touch_score)
            .collect();
        prop_assert!(scores.windows(2).all(|pair| pair[0] >= pair[1]));
    }

    /// Whatever a correction did, one Backspace puts the document back exactly as it
    /// was — including the boundary character.
    #[test]
    fn a_correction_followed_by_backspace_restores_the_document(keys in "[a-z]{1,10}") {
        let (mut engine, mut doc, _) = typed(InputMethod::Telex, &keys);
        if !engine.has_pending_correction() {
            return Ok(());
        }
        let before = doc.text().to_owned();
        doc.edited(&engine.apply_correction(Some(0)));
        doc.edited(&engine.on_backspace());
        prop_assert_eq!(doc.text(), &before);
    }

    /// Declining leaves the document exactly where the engine would have left it on
    /// its own, and nothing is parked afterwards.
    #[test]
    fn declining_matches_the_engine_without_the_feature(keys in "[a-z]{1,10}") {
        let (mut engine, mut doc, _) = typed(InputMethod::Telex, &keys);
        doc.edited(&engine.apply_correction(None));
        prop_assert!(!engine.has_pending_correction());

        let mut plain = Engine::new();
        let mut expected = Document::new();
        for key in keys.chars().chain(std::iter::once(' ')) {
            let result = plain.process_char(key);
            expected.typed(key, &result);
        }
        prop_assert_eq!(doc.text(), expected.text());
    }

    /// Nothing the search does leaves an edit half-applied.
    #[test]
    fn an_unanswered_correction_never_outlives_the_next_key(keys in "[a-z]{1,10}") {
        let (mut engine, _, _) = typed(InputMethod::Telex, &keys);
        let result = engine.process_char('a');
        prop_assert!(!engine.has_pending_correction());
        prop_assert!(result.action == Action::None || result.backspace > 0);
    }
}
