//! The faults of fast typing, repaired: a key that bounced, a modifier that strayed,
//! two fingers out of order, a key that never registered — and the words those
//! readings compete with, named, because an honest "too close to call" is the
//! answer the design asks for whenever two real words are a cheap edit apart.

use funput_core::InputMethod;
use funput_engine::{CorrectionVerdict, Engine};

use crate::support::{Document, candidate_texts, correcting_engine, index_of, type_touched};

/// Type `keys` and the space, every key reporting a touch with no neighbour on offer
/// — a finger square on its key, which leaves only the structural readings.
fn typed_square(method: InputMethod, keys: &str) -> Engine {
    let mut engine = correcting_engine(method);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, keys, &[]);
    type_touched(&mut engine, &mut doc, " ", &[]);
    engine
}

/// Type `keys` and the space with no touch reported at all: the layout speaks.
fn typed_blind(method: InputMethod, keys: &str) -> Engine {
    let mut engine = correcting_engine(method);
    for key in keys.chars().chain(std::iter::once(' ')) {
        engine.process_char(key);
    }
    engine
}

fn verdict(engine: &Engine) -> CorrectionVerdict {
    engine.rank_correction(&[], &[]).verdict
}

#[test]
fn a_bounced_key_is_the_cheapest_reading_and_applies_on_its_own() {
    let engine = typed_square(InputMethod::Telex, "chowii");
    assert_eq!(candidate_texts(&engine), ["chơi"]);
    assert_eq!(engine.correction_candidates()[0].edits(), 1);
    assert_eq!(verdict(&engine), CorrectionVerdict::Apply);

    let engine = typed_square(InputMethod::Vni, "cho7ii");
    assert_eq!(candidate_texts(&engine), ["chơi"]);
    assert_eq!(verdict(&engine), CorrectionVerdict::Apply);
}

#[test]
fn without_touches_the_layout_adds_rivals_a_key_away() {
    // Nobody saw the fingers, so `chôi` — the last `i` read as the `o` beside it on
    // the grid, at the flat layout cost — is a reading too, and a hair cheaper than
    // the bounce. Both are shown; neither is applied over the other.
    let mut engine = typed_blind(InputMethod::Telex, "chowii");
    let texts: Vec<String> = candidate_texts(&engine)
        .iter()
        .map(|text| (*text).to_owned())
        .collect();
    assert_eq!(&texts[..2], ["chôi", "chơi"]);
    assert_eq!(verdict(&engine), CorrectionVerdict::Ambiguous);
    // The shipped list knowing `chơi` and not `chôi` is worth 1.6 nats, a hair short
    // of the margin over a 0.3-nat gap; a word the user has typed before clears it.
    let shipped: Vec<u32> = texts.iter().map(|t| u32::from(t == "chơi") * 4).collect();
    assert_eq!(engine.choose_correction(&shipped, &[]), None);
    let familiar: Vec<u32> = texts.iter().map(|t| u32::from(t == "chơi") * 40).collect();
    assert_eq!(engine.choose_correction(&familiar, &[]), Some(1));
}

#[test]
fn a_stray_modifier_is_dropped_but_a_word_it_makes_is_kept() {
    // `chsowi`: the `s` landed before the vowel it could have toned.
    let engine = typed_square(InputMethod::Telex, "chsowi");
    let texts = candidate_texts(&engine);
    assert_eq!(texts[0], "chơi");
    // Moved one key along, that `s` tones the word: `chới` is a word too, one
    // transposition away, so the engine offers both rather than guessing.
    assert!(texts.contains(&"chới"), "{texts:?}");
    assert_eq!(verdict(&engine), CorrectionVerdict::Ambiguous);

    // `chowis` is not a fault at all: it *is* `chới`, and a finished syllable is
    // never touched.
    let engine = typed_square(InputMethod::Telex, "chowis");
    assert!(!engine.has_pending_correction());
}

#[test]
fn two_keys_out_of_order_are_swapped_back() {
    let engine = typed_square(InputMethod::Telex, "cohwi");
    assert_eq!(candidate_texts(&engine)[0], "chơi");
    assert_eq!(index_of(&engine, "chơi"), 0);
    assert_eq!(engine.correction_candidates()[0].edits(), 1);
}

#[test]
fn a_missing_key_is_the_dearest_reading_and_never_the_only_one() {
    // `chwi`: the `o` never registered. Dropping the stray `w` instead gives `chi`,
    // which is cheaper and a word; the insertion is offered beside it, not over it.
    let engine = typed_square(InputMethod::Telex, "chwi");
    let texts = candidate_texts(&engine);
    assert_eq!(texts[0], "chi");
    assert!(texts.contains(&"chơi"), "{texts:?}");
    assert_eq!(verdict(&engine), CorrectionVerdict::Ambiguous);
}

#[test]
fn a_word_two_insertions_short_is_out_of_reach() {
    // `chuon` reaches `chuôn` and `chươn` with one key each, at the same cost —
    // too close to call — and `chương` would take two, which no tier tries.
    let engine = typed_square(InputMethod::Telex, "chuon");
    let texts = candidate_texts(&engine);
    assert!(texts.contains(&"chuôn"), "{texts:?}");
    assert!(texts.contains(&"chươn"), "{texts:?}");
    assert!(!texts.contains(&"chương"), "{texts:?}");
    assert_eq!(verdict(&engine), CorrectionVerdict::Ambiguous);
}

#[test]
fn a_finished_syllable_is_never_offered_a_structural_reading() {
    for keys in ["nhaf", "chowi", "chwoi"] {
        let engine = typed_square(InputMethod::Telex, keys);
        assert!(!engine.has_pending_correction(), "{keys}");
    }
}

#[test]
fn two_substitutions_on_the_layout_reach_khong_in_the_last_tier() {
    let engine = typed_blind(InputMethod::Telex, "khpomg");
    assert_eq!(candidate_texts(&engine)[0], "không");
    assert_eq!(engine.correction_candidates()[0].edits(), 2);

    // A host that reported every touch and offered no neighbour for any of them
    // said the fingers were square on their keys, and is believed: nothing can be
    // swapped, and the structural edits do not reach a word.
    let engine = typed_square(InputMethod::Telex, "khpomg");
    assert!(!engine.has_pending_correction());
}

#[test]
fn a_word_typed_with_no_touch_data_at_all_still_gets_candidates() {
    let engine = typed_blind(InputMethod::Vni, "d9u7o7nh2");
    assert_eq!(candidate_texts(&engine)[0], "đường");
    assert_ne!(verdict(&engine), CorrectionVerdict::AsTyped);
}

#[test]
fn a_word_missing_one_touch_is_never_kept_as_typed() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "chowi", &[]);
    engine.process_char('i'); // no touch for the bounce
    type_touched(&mut engine, &mut doc, " ", &[]);
    assert!(candidate_texts(&engine).contains(&"chơi"));
    assert_ne!(verdict(&engine), CorrectionVerdict::AsTyped);
}
