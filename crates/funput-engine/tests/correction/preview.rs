//! Asking before the word ends: the preview, the ranking it feeds, and the refusal.

use funput_core::InputMethod;
use funput_engine::{Action, CorrectionVerdict, Engine, KeyTouch};

use crate::support::{Document, candidate_texts, correcting_engine, type_leaning, type_touched};

/// Type `keys` with the slips given, then ask for a preview.
fn previewed(keys: &str, slips: &[(usize, char)]) -> (Engine, Document, usize) {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, keys, slips);
    let found = engine.preview_correction();
    (engine, doc, found)
}

#[test]
fn a_preview_offers_what_the_boundary_would() {
    let (mut engine, mut doc, found) = previewed("dduwowfnh", &[(8, 'g')]);
    assert!(found > 0);
    assert_eq!(candidate_texts(&engine)[0], "đường");
    let previewed: Vec<_> = engine
        .correction_candidates()
        .iter()
        .map(|c| (c.text().to_owned(), c.touch_score(), c.edits()))
        .collect();
    assert!(
        !engine.has_pending_correction(),
        "nothing is parked by a preview"
    );

    let mut fresh = correcting_engine(InputMethod::Telex);
    let mut fresh_doc = Document::new();
    type_touched(&mut fresh, &mut fresh_doc, "dduwowfnh ", &[(8, 'g')]);
    let at_boundary: Vec<_> = fresh
        .correction_candidates()
        .iter()
        .map(|c| (c.text().to_owned(), c.touch_score(), c.edits()))
        .collect();
    assert_eq!(previewed, at_boundary);

    // And the boundary after a preview parks the same thing.
    type_touched(&mut engine, &mut doc, " ", &[]);
    assert!(engine.has_pending_correction());
    assert_eq!(candidate_texts(&engine)[0], "đường");
    assert_eq!(engine.pending_correction_backspace(), 10);
    doc.edited(&engine.apply_correction(Some(0)));
    assert_eq!(doc.text(), "đường ");
}

#[test]
fn the_boundary_searches_once_for_a_previewed_word() {
    let (mut engine, mut doc, _) = previewed("dduwowfnh", &[(8, 'g')]);
    let after_preview = engine.correction_metrics();
    type_touched(&mut engine, &mut doc, " ", &[]);
    let after_boundary = engine.correction_metrics();
    assert_eq!(after_boundary.offered, after_preview.offered + 1);
    assert_eq!(
        after_boundary.candidates_max, after_preview.candidates_max,
        "the search statistics did not move: the boundary reused the preview"
    );
}

#[test]
fn a_word_that_could_still_become_a_syllable_is_not_previewed() {
    // `dduwow` is `đươ`, one key short of `đường`; `chaof` is `chào` and finished.
    for (keys, slips) in [("dduwow", &[(5, 'g')][..]), ("chaof", &[(4, 's')][..])] {
        let (engine, _, found) = previewed(keys, slips);
        assert_eq!(found, 0, "{keys}");
        assert!(engine.correction_candidates().is_empty(), "{keys}");
    }
}

#[test]
fn the_next_letter_drops_the_preview() {
    let (mut engine, mut doc, _) = previewed("dduwowfnh", &[(8, 'g')]);
    assert!(!engine.correction_candidates().is_empty());
    type_touched(&mut engine, &mut doc, "a", &[]);
    assert!(engine.correction_candidates().is_empty());
    assert_eq!(
        engine.rank_correction(&[], &[]).verdict,
        CorrectionVerdict::Refused
    );
}

#[test]
fn asking_twice_answers_from_the_first_search() {
    let (mut engine, _, found) = previewed("dduwowfnh", &[(8, 'g')]);
    let once = engine.correction_metrics();
    assert_eq!(engine.preview_correction(), found);
    assert_eq!(engine.correction_metrics(), once);
}

#[test]
fn backspace_drops_the_preview_and_the_next_word_is_searched_afresh() {
    let (mut engine, mut doc, _) = previewed("dduwowfnh", &[(8, 'g')]);
    assert_eq!(candidate_texts(&engine)[0], "đường");
    engine.on_backspace();
    assert!(
        engine.correction_candidates().is_empty(),
        "the preview was about a word that no longer exists"
    );
    // A new word starts clean.
    type_touched(&mut engine, &mut doc, " dduwowfnh", &[(9, 'g')]);
    assert!(engine.preview_correction() > 0);
    assert_eq!(candidate_texts(&engine)[0], "đường");
}

#[test]
fn refusing_the_preview_keeps_the_word_and_remembers_it_once() {
    let (mut engine, mut doc, _) = previewed("dduwowfnh", &[(8, 'g')]);
    engine.refuse_correction();
    assert!(engine.correction_candidates().is_empty());
    type_touched(&mut engine, &mut doc, " ", &[]);
    assert!(!engine.has_pending_correction());
    assert_eq!(doc.text(), "dduwowfnh ");
    assert_eq!(engine.correction_metrics().suppressed, 1);

    // Typed the same way again later, it gets a fresh chance.
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    assert!(engine.has_pending_correction());
}

#[test]
fn refusing_a_parked_correction_declines_it_and_remembers_the_word() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    engine.refuse_correction();
    assert!(!engine.has_pending_correction());
    assert_eq!(engine.apply_correction(Some(0)).action, Action::None);
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    assert!(!engine.has_pending_correction());
    assert_eq!(engine.correction_metrics().suppressed, 1);
}

#[test]
fn refusing_with_nothing_in_hand_changes_nothing() {
    let mut engine = correcting_engine(InputMethod::Telex);
    engine.refuse_correction();
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    assert!(engine.has_pending_correction());
}

#[test]
fn ranking_names_the_runner_up_when_the_two_are_too_close_to_call() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_leaning(&mut engine, &mut doc, "nhad", 3, ('f', 0.3), ('s', 0.3));
    assert!(engine.preview_correction() >= 2);
    assert_eq!(&candidate_texts(&engine)[..2], ["nhà", "nhá"]);

    let ranking = engine.rank_correction(&[], &[]);
    assert_eq!(ranking.verdict, CorrectionVerdict::Ambiguous);
    assert_eq!((ranking.best, ranking.second), (Some(0), Some(1)));

    let ranking = engine.rank_correction(&[40, 0], &[]);
    assert_eq!(ranking.verdict, CorrectionVerdict::Apply);
    assert_eq!(ranking.best, Some(0));
    assert_eq!(ranking.second, Some(1));

    let none = vec![false; engine.correction_candidates().len()];
    let ranking = engine.rank_correction(&[], &none);
    assert_eq!(ranking.verdict, CorrectionVerdict::Refused);
    assert_eq!(ranking.best, None);
}

#[test]
fn ranking_a_preview_counts_nothing_but_choosing_at_the_boundary_does() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_leaning(&mut engine, &mut doc, "nhad", 3, ('f', 0.3), ('s', 0.3));
    engine.preview_correction();
    engine.rank_correction(&[], &[]);
    assert_eq!(engine.choose_correction(&[], &[]), None);
    assert_eq!(engine.correction_metrics().skipped_ambiguous, 0);

    type_touched(&mut engine, &mut doc, " ", &[]);
    assert_eq!(engine.choose_correction(&[], &[]), None);
    assert_eq!(engine.correction_metrics().skipped_ambiguous, 1);
}

#[test]
fn a_word_typed_squarely_previews_as_kept() {
    // `abh` is one key from `anh`, but every finger sat dead on the key it hit.
    let mut engine = correcting_engine(InputMethod::Vni);
    let mut doc = Document::new();
    let touches = [
        KeyTouch::new('a', 0.0).with_alternate('s', 1.0),
        KeyTouch::new('b', 0.05).with_alternate('n', 0.95),
        KeyTouch::new('h', 0.0).with_alternate('g', 1.0),
    ];
    for (key, touch) in "abh".chars().zip(touches) {
        engine.set_next_key_touch(touch);
        doc.typed(key, &engine.process_char(key));
    }
    assert!(engine.preview_correction() > 0);
    assert_eq!(
        engine.rank_correction(&[], &[]).verdict,
        CorrectionVerdict::AsTyped
    );
}

#[test]
fn an_undone_word_is_not_previewed_either() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    doc.edited(&engine.apply_correction(Some(0)));
    doc.edited(&engine.on_backspace());
    type_touched(&mut engine, &mut doc, "dduwowfnh", &[(8, 'g')]);
    assert_eq!(engine.preview_correction(), 0);
    type_touched(&mut engine, &mut doc, " ", &[]);
    assert!(!engine.has_pending_correction());
    assert_eq!(engine.correction_metrics().suppressed, 1);
}

#[test]
fn clearing_the_engine_drops_the_preview() {
    let (mut engine, _, _) = previewed("dduwowfnh", &[(8, 'g')]);
    engine.clear();
    assert!(engine.correction_candidates().is_empty());
}
