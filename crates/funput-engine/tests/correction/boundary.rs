//! How correction sits beside the other work a word boundary does.

use funput_core::InputMethod;

use crate::support::{Document, correcting_engine, type_touched};

#[test]
fn a_shortcut_still_wins_over_a_correction() {
    let mut engine = correcting_engine(InputMethod::Telex);
    engine.add_shortcut("vnn", "Việt Nam");
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "vnn ", &[(2, 'm')]);
    assert!(!engine.has_pending_correction());
    assert_eq!(doc.text(), "Việt Nam ");
}

#[test]
fn auto_capitalize_still_advances_across_a_parked_correction() {
    let mut engine = correcting_engine(InputMethod::Telex);
    engine.update_config(|config| config.auto_capitalize = true);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh. ", &[(8, 'g')]);
    type_touched(&mut engine, &mut doc, "nhaf", &[]);
    assert_eq!(
        engine.buffer(),
        "Nhà",
        "the sentence start still capitalizes"
    );
}

#[test]
fn a_word_typed_while_composition_is_off_is_never_corrected() {
    let mut engine = correcting_engine(InputMethod::Telex);
    engine.set_enabled(false);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "dduwowfnh ", &[(8, 'g')]);
    assert!(!engine.has_pending_correction());
    assert_eq!(doc.text(), "dduwowfnh ");
}

#[test]
fn re_opening_a_committed_word_puts_correction_aside() {
    // `adopt` invents the raw keys from the text, so there is no touch behind them.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "chaof ", &[]);
    assert!(engine.adopt("chào"));
    type_touched(&mut engine, &mut doc, "d ", &[(0, 'f')]);
    assert!(!engine.has_pending_correction());
}
