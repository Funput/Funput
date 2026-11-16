//! `Engine::expand_shortcut`: gõ tắt for a key the host sends on itself (Enter,
//! Tab), so the expansion comes back without a boundary character.

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use crate::support::Document;

fn engine_with(shortcuts: &[(&str, &str)]) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    for (trigger, expansion) in shortcuts {
        engine.add_shortcut(*trigger, *expansion);
    }
    engine
}

fn type_into(engine: &mut Engine, doc: &mut Document, keys: &str) {
    for key in keys.chars() {
        let result = engine.process_char(key);
        doc.typed(key, &result);
    }
}

/// Type `keys`, then press the host key: what the app shows before that key lands.
fn expanded(engine: &mut Engine, keys: &str) -> String {
    let mut doc = Document::new();
    type_into(engine, &mut doc, keys);
    if let Some(result) = engine.expand_shortcut() {
        assert_eq!(result.action, Action::Send);
        doc.edited(&result);
    }
    doc.text().to_owned()
}

#[test]
fn a_trigger_expands_without_a_boundary_character() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    assert_eq!(expanded(&mut engine, "ct"), "cơm tấm");
    assert_eq!(engine.buffer(), "", "the word is committed");
}

#[test]
fn a_word_that_is_no_trigger_is_left_alone() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    assert_eq!(engine.expand_shortcut(), None);
    assert_eq!(expanded(&mut engine, "cows"), "cớ");
    assert_eq!(
        engine.buffer(),
        "cớ",
        "the host commits it as for any caret key"
    );
}

/// Smart case reaches Enter and Tab the way it reaches Space.
#[test]
fn the_expansion_follows_smart_case() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    assert_eq!(expanded(&mut engine, "Ct"), "Cơm tấm");
}

/// By the time Backspace could ask, the line is sent or the focus has moved on.
#[test]
fn no_undo_is_armed() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    expanded(&mut engine, "ct");
    assert!(!engine.has_shortcut_undo());
}

/// A trigger the user just took back stays as typed, as it would at Space.
#[test]
fn an_undone_trigger_stays_as_typed() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "ct ");
    let undo = engine.undo_shortcut().expect("undo armed");
    doc.edited(&undo);
    // Backspace over the space re-opens the trigger, as the hosts do.
    assert!(engine.adopt("ct"));

    assert_eq!(engine.expand_shortcut(), None);
}

/// A word glued to a number is a unit on it (`500k`), never a trigger.
#[test]
fn a_unit_glued_to_a_number_does_not_expand() {
    let mut engine = engine_with(&[("k", "không")]);
    assert_eq!(expanded(&mut engine, "500k"), "500k");
}

#[test]
fn nothing_expands_with_gõ_tắt_off() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    engine.update_config(|c| c.shortcuts_enabled = false);
    assert_eq!(expanded(&mut engine, "ct"), "ct");
}

/// English mode expands only when gõ tắt is set to run there.
#[test]
fn english_mode_follows_the_english_shortcuts_switch() {
    let mut engine = engine_with(&[("ct", "cơm tấm")]);
    engine.set_enabled(false);
    engine.update_config(|c| c.shortcuts_in_english = false);
    assert_eq!(expanded(&mut engine, "ct"), "ct");

    engine.clear();
    engine.update_config(|c| c.shortcuts_in_english = true);
    assert_eq!(expanded(&mut engine, "ct"), "cơm tấm");
}
