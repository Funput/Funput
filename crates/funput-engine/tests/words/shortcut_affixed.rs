//! Gõ tắt triggers that carry symbols: a prefix typed in front of the word (`;ct`,
//! `2k`) and a suffix that is the boundary key itself (`ct;`). The longest spelling
//! wins, and the symbols are swallowed with the word. Only a host that applies a
//! result verbatim turns this on (`shortcut_affixes`). See
//! `docs/features/shortcut-glued-words.md`.

use funput_core::InputMethod;
use funput_engine::Engine;

use crate::support::{Document, app_text_in};

fn engine_with(shortcuts: &[(&str, &str)]) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    engine.update_config(|config| config.shortcut_affixes = true);
    for (trigger, expansion) in shortcuts {
        engine.add_shortcut(*trigger, *expansion);
    }
    engine
}

fn typed(shortcuts: &[(&str, &str)], keys: &str) -> String {
    app_text_in(&mut engine_with(shortcuts), keys)
}

/// Off by default: a host that cannot delete committed text never sees a prefix
/// or suffix result, and the symbols stay as typed.
#[test]
fn a_host_that_has_not_opted_in_never_reaches_an_affixed_trigger() {
    let mut engine = Engine::new();
    for (trigger, expansion) in [(";ct", "cơm tấm"), ("ct;", "cơm tấm"), ("ct", "công ty")] {
        engine.add_shortcut(trigger, expansion);
    }
    assert_eq!(app_text_in(&mut engine, ";ct "), ";ct ");
    engine.clear();
    assert_eq!(app_text_in(&mut engine, "ct;"), "công ty;");
}

#[test]
fn a_prefixed_trigger_swallows_its_prefix() {
    let table = [(";ct", "cơm tấm")];
    assert_eq!(typed(&table, ";ct "), "cơm tấm ");
    assert_eq!(typed(&table, "an ;ct "), "an cơm tấm ");
    assert_eq!(typed(&table, ";ct."), "cơm tấm.");
}

/// A bare trigger glued behind the prefix stays as typed: only the prefixed
/// spelling reaches it.
#[test]
fn a_prefix_the_table_does_not_know_glues_the_word() {
    assert_eq!(typed(&[("ct", "cơm tấm")], ";ct "), ";ct ");
}

#[test]
fn the_longest_spelling_wins() {
    let table = [("ct", "cơm tấm"), (";ct", "công ty")];
    assert_eq!(typed(&table, "ct "), "cơm tấm ");
    assert_eq!(typed(&table, ";ct "), "công ty ");
}

#[test]
fn a_suffixed_trigger_swallows_its_boundary_key() {
    let table = [("ct;", "cơm tấm"), ("ct", "công ty")];
    assert_eq!(typed(&table, "ct;"), "cơm tấm");
    assert_eq!(typed(&table, "ct,"), "công ty,");
    assert_eq!(typed(&table, "ct "), "công ty ");
}

#[test]
fn a_digit_prefix_reaches_its_trigger_but_a_number_stays_a_number() {
    let table = [("2k", "hai nghìn"), ("k", "không")];
    assert_eq!(typed(&table, "2k "), "hai nghìn ");
    assert_eq!(typed(&table, "500k "), "500k ");
}

#[test]
fn smart_case_reads_only_the_letters() {
    let table = [(";ct", "cơm tấm")];
    assert_eq!(typed(&table, ";Ct "), "Cơm tấm ");
    assert_eq!(typed(&table, ";CT "), "CƠM TẤM ");
}

/// In advanced Telex `[` composes `ư`: it is part of the word, never a prefix, so
/// `[ct` is reached as one word and `ct` behind it is not glued to anything.
#[test]
fn an_advanced_telex_bracket_is_a_letter_not_a_prefix() {
    let mut engine = engine_with(&[("[ct", "cơm tấm")]);
    engine.set_method(InputMethod::TelexAdvanced);
    assert_eq!(app_text_in(&mut engine, "[ct "), "cơm tấm ");
}

/// Backspace right after the expansion puts back the prefix with the word.
#[test]
fn undo_puts_the_prefix_back() {
    let mut engine = engine_with(&[(";ct", "cơm tấm")]);
    let mut doc = Document::new();
    for key in ";ct ".chars() {
        let result = engine.process_char(key);
        doc.typed(key, &result);
    }
    assert_eq!(doc.text(), "cơm tấm ");
    let undo = engine.undo_shortcut().expect("undo armed");
    doc.edited(&undo);
    assert_eq!(doc.text(), ";ct ");
}

/// Backspace right after a suffixed expansion puts back the word and its suffix.
#[test]
fn undo_puts_the_suffix_back() {
    let mut engine = engine_with(&[("ct;", "cơm tấm")]);
    let mut doc = Document::new();
    for key in "ct;".chars() {
        let result = engine.process_char(key);
        doc.typed(key, &result);
    }
    assert_eq!(doc.text(), "cơm tấm");
    let undo = engine.undo_shortcut().expect("undo armed");
    doc.edited(&undo);
    assert_eq!(doc.text(), "ct;");
}

/// A host re-opens only the word behind the caret, never the symbols in front of
/// it, so Backspace into a prefixed trigger leaves it committed (a known limit).
#[test]
fn a_prefixed_trigger_is_not_reopened_without_its_prefix() {
    let mut engine = engine_with(&[(";ctsg", "cơm tấm Sài Gòn")]);
    assert!(!engine.adopt_after("cts", Some(';')));
}
