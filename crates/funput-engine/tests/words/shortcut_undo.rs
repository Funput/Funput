//! One Backspace puts a gõ tắt expansion back, boundary character included.

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use crate::support::{Document, correcting_engine, type_touched};

fn engine_with(shortcuts: &[(&str, &str)]) -> Engine {
    let mut engine = Engine::new();
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

fn undo(engine: &mut Engine, doc: &mut Document) {
    let result = engine.undo_shortcut().expect("undo armed");
    assert_eq!(result.action, Action::Send);
    doc.edited(&result);
}

#[test]
fn backspace_restores_the_shown_word_and_keeps_the_space() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    assert_eq!(doc.text(), "Việt Nam ");
    assert_eq!(engine.shortcut_undo_text(), Some("vn"));
    assert_eq!(engine.shortcut_undo_written(), Some("Việt Nam "));

    undo(&mut engine, &mut doc);

    assert_eq!(doc.text(), "vn ");
    assert!(!engine.has_shortcut_undo());
}

#[test]
fn a_capital_trigger_comes_back_as_typed() {
    let mut engine = engine_with(&[("a", "anh")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "A ");
    assert_eq!(doc.text(), "Anh ");
    assert_eq!(engine.shortcut_undo_text(), Some("A"));

    undo(&mut engine, &mut doc);

    assert_eq!(doc.text(), "A ");
}

#[test]
fn a_composed_trigger_comes_back_as_shown_not_as_raw_keys() {
    let mut engine = engine_with(&[("đc", "được")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "ddc ");
    assert_eq!(doc.text(), "được ");
    assert_eq!(engine.shortcut_undo_text(), Some("đc"));

    undo(&mut engine, &mut doc);

    assert_eq!(doc.text(), "đc ");
}

#[test]
fn punctuation_stays_with_the_restored_word() {
    let mut engine = engine_with(&[("kg", "không")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "kg,");
    assert_eq!(doc.text(), "không,");

    undo(&mut engine, &mut doc);

    assert_eq!(doc.text(), "kg,");
}

#[test]
fn the_next_key_spends_the_undo_without_refusing_the_trigger() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    type_into(&mut engine, &mut doc, "x");
    assert!(!engine.has_shortcut_undo());

    type_into(&mut engine, &mut doc, " vn ");
    assert_eq!(doc.text(), "Việt Nam x Việt Nam ");
}

#[test]
fn the_refused_trigger_is_kept_once_and_then_expands_again() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    type_into(&mut engine, &mut doc, "vn ");
    assert_eq!(doc.text(), "vn vn ");
    assert!(!engine.has_shortcut_undo());

    type_into(&mut engine, &mut doc, "vn ");
    assert_eq!(doc.text(), "vn vn Việt Nam ");
}

#[test]
fn a_different_trigger_still_expands_right_after_one_is_refused() {
    let mut engine = engine_with(&[("vn", "Việt Nam"), ("kg", "không")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    type_into(&mut engine, &mut doc, "kg ");

    assert_eq!(doc.text(), "vn không ");
}

/// The refusal is for the next word only. Left standing, an `a` → `anh` undone in
/// one sentence would silently fail to expand in a later one.
#[test]
fn the_refusal_is_spent_by_the_next_word_even_when_it_is_another() {
    let mut engine = engine_with(&[("vn", "Việt Nam"), ("kg", "không")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    type_into(&mut engine, &mut doc, "kg ");
    type_into(&mut engine, &mut doc, "vn ");

    assert_eq!(doc.text(), "vn không Việt Nam ");
}

#[test]
fn the_refusal_does_not_outlive_an_ordinary_sentence() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    type_into(&mut engine, &mut doc, "toi o day, hom nay moi ngay ");
    type_into(&mut engine, &mut doc, "vn ");

    assert!(doc.text().ends_with("ngay Việt Nam "), "{}", doc.text());
}

/// A second space ends no word, so it must not eat the refusal meant for the
/// word retyped after it.
#[test]
fn an_empty_boundary_leaves_the_refusal_standing() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    type_into(&mut engine, &mut doc, " ");
    type_into(&mut engine, &mut doc, "vn ");

    assert_eq!(doc.text(), "vn  vn ");
}

/// A refused trigger stands down gõ tắt and typo correction, not English restore:
/// with eager restore off, `text` is only turned back into `text` at the boundary.
#[test]
fn a_refused_trigger_still_gets_english_restore() {
    for eager in [true, false] {
        let mut engine = engine_with(&[("text", "văn bản")]);
        engine.update_config(|config| config.eager_restore = eager);
        let mut doc = Document::new();
        type_into(&mut engine, &mut doc, "text ");
        undo(&mut engine, &mut doc);
        type_into(&mut engine, &mut doc, "text ");

        assert_eq!(doc.text(), "text text ", "eager restore {eager}");
    }
}

#[test]
fn clearing_the_table_keeps_the_undo_that_the_screen_still_needs() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    engine.clear_shortcuts();
    assert!(engine.has_shortcut_undo());

    undo(&mut engine, &mut doc);

    assert_eq!(doc.text(), "vn ");
}

#[test]
fn turning_gõ_tắt_off_leaves_a_refusal_harmless() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    engine.update_config(|config| config.shortcuts_enabled = false);
    type_into(&mut engine, &mut doc, "vn ");
    engine.update_config(|config| config.shortcuts_enabled = true);
    type_into(&mut engine, &mut doc, "vn ");

    assert_eq!(doc.text(), "vn vn Việt Nam ");
}

#[test]
fn a_refused_trigger_is_not_typo_corrected() {
    let mut control = correcting_engine(InputMethod::Telex);
    let mut control_doc = Document::new();
    type_touched(&mut control, &mut control_doc, "tpoi ", &[(1, 'o')]);
    assert!(
        control.has_pending_correction(),
        "the control word has to be one correction would repair"
    );

    let mut engine = correcting_engine(InputMethod::Telex);
    engine.add_shortcut("tpoi", "tôi");
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "tpoi ", &[(1, 'o')]);
    assert_eq!(doc.text(), "tôi ");
    assert!(!engine.has_pending_correction());
    undo(&mut engine, &mut doc);
    type_touched(&mut engine, &mut doc, "tpoi ", &[(1, 'o')]);

    assert!(!engine.has_pending_correction());
    assert_eq!(doc.text(), "tpoi tpoi ");
}

#[test]
fn backspace_itself_does_not_take_the_undo() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");

    let result = engine.on_backspace();

    assert_eq!(result.action, Action::None);
    assert_eq!(engine.shortcut_undo_text(), Some("vn"));
    undo(&mut engine, &mut doc);
    assert_eq!(doc.text(), "vn ");
}

#[test]
fn clearing_the_engine_drops_the_undo_and_the_refusal() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    undo(&mut engine, &mut doc);
    engine.clear();
    type_into(&mut engine, &mut doc, "vn ");

    assert_eq!(doc.text(), "vn Việt Nam ");
}

#[test]
fn english_mode_undoes_the_same_way() {
    let mut engine = engine_with(&[("vn", "Việt Nam")]);
    engine.set_enabled(false);
    let mut doc = Document::new();
    type_into(&mut engine, &mut doc, "vn ");
    assert_eq!(doc.text(), "Việt Nam ");
    assert_eq!(engine.shortcut_undo_text(), Some("vn"));

    undo(&mut engine, &mut doc);

    assert_eq!(doc.text(), "vn ");
}
