//! What correction refuses to look at. Every case here is a word the engine must
//! leave exactly as it is today.

use funput_core::InputMethod;

use super::{engine, type_and_end, type_word};
use crate::Engine;

#[test]
fn a_valid_syllable_is_never_offered_a_correction() {
    // `chaof` is `chào`, typed exactly right — with a neighbour on offer anyway.
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "chaof", &[(4, 's')]);
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_bare_shaped_vowel_is_left_alone() {
    // `aw` → `ă` is not a syllable, but it is the letter the user asked for.
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "aw", &[(1, 's')]);
    assert!(!engine.has_pending_correction());
}

#[test]
fn an_all_uppercase_word_is_skipped() {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "DDUWOWFNH", &[(8, 'g')]);
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_word_containing_a_digit_is_skipped() {
    // Without the digit this is the `nhad` case, which does get `nhá` offered.
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "nha2", &[(3, 's')]);
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_word_with_no_touch_data_is_skipped() {
    let mut engine = engine(InputMethod::Telex);
    for key in "dduwowfnh ".chars() {
        engine.process_char(key);
    }
    assert!(!engine.has_pending_correction());
}

#[test]
fn one_key_without_touch_data_spoils_the_whole_word() {
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "dduwowfn", &[]);
    engine.process_char('h'); // no touch reported for this one
    engine.process_char(' ');
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_flipped_word_is_left_alone() {
    // The user pinned this form by hand; it is a choice, not a slip.
    let mut engine = engine(InputMethod::Telex);
    type_word(&mut engine, "dduwowfnh", &[(8, 'g')]);
    engine.flip_composing();
    engine.process_char(' ');
    assert!(!engine.has_pending_correction());
}

#[test]
fn the_feature_switched_off_offers_nothing() {
    let mut engine = Engine::new();
    assert!(!engine.config().typo_correction);
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    assert!(!engine.has_pending_correction());
    assert!(engine.correction_candidates().is_empty());
}

#[test]
fn an_english_word_the_user_typed_on_purpose_still_reaches_the_platform() {
    // `text` ends as raw keys exactly the way a mistyped Vietnamese word does, and
    // `r` sits next to `t`, so the engine can reach `tẻ` from it. The engine has no
    // English lexicon to know better — what keeps `text` intact is the platform's
    // veto, and the point of this test is that the engine defers rather than deciding.
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "text", &[(3, 'r')]);
    assert!(engine.has_pending_correction());
    assert_eq!(engine.apply_correction(None).output, "");
}
