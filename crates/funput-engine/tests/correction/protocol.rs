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
fn applying_a_candidate_over_a_composed_word_deletes_only_what_is_shown() {
    // `quaa` is displayed as `quâ`, not as its four keystrokes: what has to be
    // deleted is what the app shows, plus the space it has already echoed.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    assert_eq!(doc.text(), "quâ ");
    assert_eq!(engine.pending_correction_backspace(), 4);
    doc.edited(&engine.apply_correction(Some(0)));
    assert_eq!(doc.text(), "quá ");
}

#[test]
fn declining_finishes_the_english_restore_the_boundary_deferred() {
    // `quaa` composes to `quâ` and is restored to its keys at the boundary. With a
    // correction parked, that restore waits for the platform's answer — and still
    // happens, one character longer because the space is already in the document.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    assert_eq!(doc.text(), "quâ ");
    assert_eq!(candidate_texts(&engine), ["quá"]);
    doc.edited(&engine.apply_correction(None));
    assert_eq!(doc.text(), "quaa ");
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
    // The next keystroke settles the parked correction, replaying the restore the
    // boundary deferred — so a host that only implements step one still gets today's
    // behaviour, one keystroke late.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa ", &[(3, 's')]);
    type_touched(&mut engine, &mut doc, "a", &[]);
    assert_eq!(doc.text(), "quaa a");
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_platform_that_never_answers_is_settled_by_the_next_boundary_too() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "quaa  ", &[(3, 's')]);
    assert_eq!(doc.text(), "quaa  ");
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
