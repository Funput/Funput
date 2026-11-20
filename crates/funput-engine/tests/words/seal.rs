//! `Engine::seal`: the live word ends exactly as shown, with no word boundary run.

use funput_core::InputMethod;
use funput_engine::Engine;

use crate::support::{Document, correcting_engine, type_touched};

fn telex() -> Engine {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    engine
}

fn type_into(engine: &mut Engine, doc: &mut Document, keys: &str) {
    for key in keys.chars() {
        let result = engine.process_char(key);
        doc.typed(key, &result);
    }
}

/// Type `before`, seal, type `after`: what the app shows.
fn sealed_between(engine: &mut Engine, before: &str, after: &str) -> String {
    let mut doc = Document::new();
    type_into(engine, &mut doc, before);
    engine.seal();
    type_into(engine, &mut doc, after);
    doc.text().to_owned()
}

#[test]
fn the_shown_word_comes_back_and_the_composition_ends() {
    let mut engine = telex();
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "haf");
    assert_eq!(engine.seal().as_deref(), Some("hà"));
    assert_eq!(engine.buffer(), "");
    assert_eq!(doc.text(), "hà", "nothing is written");
}

#[test]
fn nothing_composing_is_nothing_to_seal() {
    let mut engine = telex();
    assert_eq!(engine.seal(), None);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "haf ");
    assert_eq!(engine.seal(), None);
}

/// The key that would have marked the word starts a new one behind it instead.
#[test]
fn the_next_key_no_longer_reaches_the_sealed_word() {
    assert_eq!(sealed_between(&mut telex(), "dung", "x"), "dungx");
    assert_eq!(sealed_between(&mut telex(), "haf", "s"), "hàs");
}

#[test]
fn a_sealed_trigger_does_not_expand() {
    let mut engine = telex();
    engine.add_shortcut("ct", "cơm tấm");
    assert_eq!(sealed_between(&mut engine, "CT", " "), "CT ");
    assert!(!engine.has_shortcut_undo());
}

/// English mode tracks the raw keys for gõ tắt, and a seal stops that too.
#[test]
fn a_sealed_trigger_does_not_expand_in_english_mode() {
    let mut engine = telex();
    engine.add_shortcut("ct", "cơm tấm");
    engine.set_enabled(false);
    assert_eq!(sealed_between(&mut engine, "ct", " "), "ct ");
}

/// `card` shows as Vietnamese until its boundary restores it; a seal keeps what shows.
#[test]
fn a_sealed_word_is_not_restored_to_english() {
    let mut engine = telex();
    engine.update_config(|config| config.eager_restore = false);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "card");
    let shown = doc.text().to_owned();
    assert_ne!(shown, "card", "the word is still in its Vietnamese form");

    assert_eq!(engine.seal().as_deref(), Some(shown.as_str()));
    type_into(&mut engine, &mut doc, " ");
    assert_eq!(doc.text(), format!("{shown} "));
}

#[test]
fn a_sealed_word_is_not_corrected() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh", &[(8, 'g')]);
    let shown = doc.text().to_owned();

    engine.seal();
    type_touched(&mut engine, &mut doc, " ", &[]);
    assert!(!engine.has_pending_correction());
    assert_eq!(doc.text(), format!("{shown} "));
}

/// No key was typed, so the sentence neither ends nor starts over.
#[test]
fn the_sentence_carries_on_across_a_seal() {
    let mut engine = telex();
    engine.update_config(|config| config.auto_capitalize = true);
    let mut doc = Document::new();
    engine.arm_capitalization();
    type_into(&mut engine, &mut doc, "chaof");
    engine.seal();
    type_into(&mut engine, &mut doc, "x. ban");
    assert_eq!(doc.text(), "Chàox. Ban");
}

/// A trigger the user took back is kept once. Sealing it is a new decision about the
/// word, so the next time it is typed it expands again.
#[test]
fn a_seal_ends_a_shortcut_refusal() {
    let mut engine = telex();
    engine.add_shortcut("ct", "cơm tấm");
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "ct ");
    let undo = engine.undo_shortcut().expect("undo armed");
    doc.edited(&undo);
    // Backspace over the space re-opens the trigger, as the hosts do.
    doc.deleted();
    assert!(engine.adopt("ct"));

    assert_eq!(engine.seal().as_deref(), Some("ct"));
    type_into(&mut engine, &mut doc, " ct ");
    assert_eq!(doc.text(), "ct cơm tấm ");
}
