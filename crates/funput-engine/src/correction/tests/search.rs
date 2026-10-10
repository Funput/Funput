//! What the replay search finds, on the words that prompted the feature.

use funput_core::InputMethod;

use super::{candidate_texts, engine, type_and_end, type_word};

#[test]
fn dduwowfnh_reaches_duong_by_replacing_h_with_g() {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    assert!(engine.has_pending_correction());
    assert_eq!(candidate_texts(&engine), ["đường"]);
}

#[test]
fn vni_d9u7o7nh2_reaches_duong_through_the_digit_modifiers() {
    let mut engine = engine(InputMethod::Vni);
    type_and_end(&mut engine, "d9u7o7nh2", &[(7, 'g')]);
    assert_eq!(candidate_texts(&engine), ["đường"]);
}

#[test]
fn nhad_offers_nha_with_either_tone() {
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "nhad", &[(3, 'f'), (3, 's')]);
    engine.process_char(' ');
    let mut texts = candidate_texts(&engine);
    texts.sort_unstable();
    assert_eq!(texts, ["nhà", "nhá"]);
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
    type_and_end(&mut engine, "dduwowfnhxx", &[(8, 'g')]);
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_word_no_neighbour_can_rescue_is_left_alone() {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'k')]);
    assert!(!engine.has_pending_correction());
}
