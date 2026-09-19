//! The one-tap undo behind an applied correction.

use funput_core::InputMethod;

use super::{engine, type_and_end};
use crate::Action;

/// Type `dduwowfnh `, take the correction, and hand back the engine mid-undo-window.
fn corrected() -> crate::Engine {
    let mut engine = engine(InputMethod::Telex);
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    let applied = engine.apply_correction(Some(0));
    assert_eq!(applied.backspace, 10, "the word plus the echoed space");
    assert_eq!(applied.output, "đường ");
    engine
}

#[test]
fn backspace_right_after_a_correction_restores_the_typed_keys() {
    let mut engine = corrected();
    assert!(engine.has_correction_undo());
    let undo = engine.on_backspace();
    assert_eq!(undo.action, Action::Send);
    assert_eq!(undo.backspace, 6, "đường plus the space it carried");
    assert_eq!(undo.output, "dduwowfnh ");
}

#[test]
fn the_undo_chip_shows_the_word_the_user_typed() {
    let engine = corrected();
    assert_eq!(engine.correction_undo_text(), Some("dduwowfnh"));
}

#[test]
fn a_keystroke_after_a_correction_disarms_the_one_tap_undo() {
    let mut engine = corrected();
    engine.process_char('a');
    assert!(!engine.has_correction_undo());
    assert_eq!(engine.on_backspace().action, Action::None);
}

#[test]
fn an_undone_word_is_not_corrected_a_second_time() {
    let mut engine = corrected();
    engine.on_backspace();
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    assert!(!engine.has_pending_correction());
}

#[test]
fn refusing_a_word_once_does_not_refuse_it_forever() {
    // A word the user rescued is given another chance next time it is typed, which
    // is how the system keyboards behave.
    let mut engine = corrected();
    engine.on_backspace();
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    type_and_end(&mut engine, "dduwowfnh", &[(8, 'g')]);
    assert!(engine.has_pending_correction());
}
