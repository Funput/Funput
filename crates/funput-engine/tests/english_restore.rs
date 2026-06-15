mod support;

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn telex_valid_vn_keeps_composed_on_space() {
    assert_eq!(support::app_text(InputMethod::Telex, "mas "), "má ");
}

#[test]
fn telex_english_restore_on_space() {
    assert_eq!(support::app_text(InputMethod::Telex, "absc "), "absc ");
}

#[test]
fn telex_pass_through_english_no_restore() {
    assert_eq!(support::app_text(InputMethod::Telex, "file "), "file ");
}

#[test]
fn telex_english_restore_on_punctuation() {
    assert_eq!(support::app_text(InputMethod::Telex, "absc,"), "absc,");
}

#[test]
fn telex_absc_restore_step() {
    let mut engine = Engine::new();
    engine.process_char('a');
    engine.process_char('b');
    let tone = engine.process_char('s');
    assert_eq!(tone.action, Action::Send);
    assert_eq!(engine.buffer(), "áb");
    assert_eq!(engine.keys(), "abs");

    engine.process_char('c');
    assert_eq!(engine.buffer(), "ábc");
    assert_eq!(engine.keys(), "absc");

    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::Send);
    assert_eq!(space.backspace, 3);
    assert_eq!(space.output, "absc ");
    assert_eq!(engine.buffer(), "");
}

#[test]
fn telex_mas_space_no_restore() {
    let mut engine = Engine::new();
    engine.process_char('m');
    engine.process_char('a');
    engine.process_char('s');
    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert!(space.output.is_empty());
}
