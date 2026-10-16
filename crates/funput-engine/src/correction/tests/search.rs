//! What the search finds, on the words that prompted the feature.

use funput_core::InputMethod;

use super::{candidate_texts, engine, type_and_end, type_word};
use crate::CorrectionVerdict;

#[test]
fn dduwowfnh_reaches_duong_by_replacing_h_with_g() {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    assert!(engine.has_pending_correction());
    // The slip the finger reported comes first; the one structural reading within
    // reach of the cheap tiers — one `d` too many, paired with the slip — trails it.
    // Dropping the `h` instead (`đườn`) lives in the last tier, which a slip this
    // clear never pays for.
    assert_eq!(candidate_texts(&engine), ["đường", "dường"]);
}

#[test]
fn vni_d9u7o7nh2_reaches_duong_through_the_digit_modifiers() {
    let mut engine = engine(InputMethod::Vni);
    type_and_end(&mut engine, "d9u7o7nh2", &[(7, 'g')]);
    assert_eq!(candidate_texts(&engine)[0], "đường");
}

#[test]
fn nhad_offers_nha_with_either_tone() {
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "nhad", &[(3, 'f'), (3, 's')]);
    engine.process_char(' ');
    assert_eq!(
        candidate_texts(&engine),
        ["nhà", "nhá"],
        "the two tones at equal cost, and nothing dearer is looked for"
    );
    assert_eq!(engine.choose_correction(&[], &[]), None);
    assert_eq!(
        engine.rank_correction(&[], &[]).verdict,
        CorrectionVerdict::Ambiguous
    );
}

#[test]
fn candidates_come_back_best_touch_score_first() {
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "nhad", &[(3, 'f'), (3, 's')]);
    engine.process_char(' ');
    let scores: Vec<f32> = engine
        .correction_candidates()
        .iter()
        .map(|candidate| candidate.touch_score())
        .collect();
    assert!(scores.windows(2).all(|pair| pair[0] >= pair[1]));
}

#[test]
fn the_search_ignores_a_word_past_the_key_cap() {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "dduwowfnhxxxx", &[(8, 'g')]); // 13 keys
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_word_no_edit_can_rescue_is_left_alone() {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "qqqq", &[(0, 'w')]);
    assert!(!engine.has_pending_correction());
    assert_eq!(engine.correction_metrics().no_candidate, 1);
}
