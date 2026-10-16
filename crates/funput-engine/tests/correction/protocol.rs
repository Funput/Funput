//! The two-step handshake: what the boundary parks, and what each answer does to the
//! document.

use funput_core::InputMethod;
use funput_engine::Action;

use crate::support::{Document, candidate_texts, correcting_engine, type_touched};

#[test]
fn a_boundary_with_candidates_returns_none_and_parks_them() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    // The keystroke itself behaved exactly as it does today.
    assert_eq!(doc.text(), "dduwowfnh ");
    assert!(engine.has_pending_correction());
    assert_eq!(candidate_texts(&engine), ["đường"]);
}

#[test]
fn applying_a_candidate_replaces_the_word_and_keeps_the_boundary() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    assert_eq!(engine.pending_correction_backspace(), 10);
    doc.edited(&engine.apply_correction(Some(0)));
    assert_eq!(doc.text(), "đường ");
    assert!(!engine.has_pending_correction());
}

#[test]
fn applying_a_candidate_over_a_restored_word_deletes_the_keys_it_shows() {
    // `quaa` composes to `quâ`, and the boundary restores it to its four keys in the
    // same keystroke, correction or not. What has to be deleted is what the app shows
    // once that keystroke is done — the keys — plus the space it has already echoed.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    assert_eq!(doc.text(), "quaa ");
    assert_eq!(candidate_texts(&engine), ["quá"]);
    assert_eq!(engine.pending_correction_backspace(), 5);
    doc.edited(&engine.apply_correction(Some(0)));
    assert_eq!(doc.text(), "quá ");
}

#[test]
fn applying_a_candidate_over_a_composed_word_deletes_only_what_is_shown() {
    // With English restore off the boundary leaves `quâ` on screen, so the same
    // correction deletes three characters, not four.
    let mut engine = correcting_engine(InputMethod::Telex);
    engine.update_config(|config| config.smart_restore = false);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    assert_eq!(doc.text(), "quâ ");
    assert_eq!(engine.pending_correction_backspace(), 4);
    doc.edited(&engine.apply_correction(Some(0)));
    assert_eq!(doc.text(), "quá ");
}

#[test]
fn declining_a_restored_word_changes_nothing_the_boundary_already_did() {
    // The English restore ran at the boundary, as it would have without the feature;
    // declining has nothing left to finish.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    assert_eq!(doc.text(), "quaa ");
    let declined = engine.apply_correction(None);
    assert_eq!(declined.action, Action::None);
    doc.edited(&declined);
    assert_eq!(doc.text(), "quaa ");
    assert!(!engine.has_pending_correction());
}

#[test]
fn declining_a_word_the_engine_would_have_left_alone_changes_nothing() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    let declined = engine.apply_correction(None);
    assert_eq!(declined.action, Action::None);
    doc.edited(&declined);
    assert_eq!(doc.text(), "dduwowfnh ");
}

#[test]
fn a_platform_that_never_answers_loses_nothing() {
    // The boundary already restored the word, so a host that only implements step
    // one gets today's behaviour in today's keystroke. The next key merely lets the
    // parked correction lapse, candidates included.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    assert_eq!(doc.text(), "quaa ");
    type_touched(&mut engine, &mut doc, "a", &[]);
    assert_eq!(doc.text(), "quaa a");
    assert!(!engine.has_pending_correction());
    assert!(engine.correction_candidates().is_empty());
}

#[test]
fn a_platform_that_never_answers_is_settled_by_the_next_boundary_too() {
    // Two boundaries in a row: the second finds nothing to correct and nothing
    // parked from the first survives it.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa  ", &[(3, 's')]);
    assert_eq!(doc.text(), "quaa  ");
    assert!(!engine.has_pending_correction());
}

#[test]
fn candidates_are_gone_once_the_correction_is_answered() {
    // A ranking must never outlive the word it was made for: once answered, there
    // is nothing to read back and nothing for `choose_correction` to rank.
    for answer in [Some(0), None] {
        let mut engine = correcting_engine(InputMethod::Telex);
        let mut doc = Document::new();
        type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
        assert_eq!(candidate_texts(&engine), ["đường"]);
        engine.apply_correction(answer);
        assert!(engine.correction_candidates().is_empty());
        assert_eq!(engine.choose_correction(&[], &[]), None);
        let metrics = engine.correction_metrics();
        assert_eq!(metrics.skipped_ambiguous + metrics.kept_as_typed, 0);
    }
}

#[test]
fn candidates_are_gone_once_the_host_clears_the_engine() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    engine.clear();
    assert!(engine.correction_candidates().is_empty());
    assert_eq!(engine.choose_correction(&[], &[]), None);
}

#[test]
fn an_answer_with_nothing_pending_is_a_no_op() {
    let mut engine = correcting_engine(InputMethod::Telex);
    assert_eq!(engine.apply_correction(Some(0)).action, Action::None);
    assert_eq!(engine.apply_correction(None).action, Action::None);
    assert_eq!(engine.pending_correction_backspace(), 0);
    assert!(engine.correction_candidates().is_empty());
}

#[test]
fn an_index_that_names_no_candidate_declines_rather_than_guessing() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    doc.edited(&engine.apply_correction(Some(99)));
    assert_eq!(doc.text(), "quaa ");
}

#[test]
fn a_host_with_no_word_store_still_gets_a_ranking() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    assert_eq!(engine.choose_correction(&[], &[]), Some(0));
}

#[test]
fn two_candidates_too_close_to_call_are_offered_rather_than_applied() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "nhad ", &[(3, 'f'), (3, 's')]);
    assert_eq!(candidate_texts(&engine).len(), 2);
    assert_eq!(engine.choose_correction(&[], &[]), None);
    // The user's own history breaks the tie.
    assert_eq!(engine.choose_correction(&[40, 0], &[]), Some(0));
}

#[test]
fn refusing_the_leading_candidate_calls_the_whole_correction_off() {
    // The safety property of an *incomplete* dictionary. A host that has never heard
    // of the right word must not thereby hand the win to a word it has heard of: a
    // refused candidate cannot win, but it still competes for the confidence margin,
    // so the one the host does not know suppresses the one it does.
    //
    // Measured over Viet74K with a 569-word list: letting the refused candidate drop
    // out of the comparison put 59.6% of corrections wrong; keeping it in puts 5.3%
    // wrong. A dictionary that knows too little now corrects less, not badly.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "nhad ", &[(3, 'f'), (3, 's')]);
    assert_eq!(candidate_texts(&engine).len(), 2);

    assert_eq!(
        engine.choose_correction(&[40, 0], &[false, true]),
        None,
        "the refused leader still outranks the candidate that was allowed"
    );
    assert_eq!(engine.choose_correction(&[], &[false, false]), None);
}

#[test]
fn refusing_a_trailing_candidate_leaves_the_leader_to_win() {
    // The other half: refusing a candidate that was losing anyway changes nothing,
    // so a dictionary only ever costs corrections it had a reason to doubt.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "nhad ", &[(3, 'f'), (3, 's')]);

    assert_eq!(engine.choose_correction(&[40, 0], &[true, false]), Some(0));
    assert_eq!(
        engine.choose_correction(&[40, 0], &[true]),
        Some(0),
        "a short mask permits the rest"
    );
}

#[test]
fn moving_the_caret_drops_a_parked_correction() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    engine.clear();
    assert!(!engine.has_pending_correction());
    assert_eq!(engine.apply_correction(Some(0)).action, Action::None);
}
