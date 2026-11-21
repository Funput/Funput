//! Gõ tắt leaves a word glued to the text in front of it alone: `vn` in `abc.vn`,
//! `t` in `don't`, `k` in `500k`. Only whitespace, the start of what the engine
//! has seen, or a lone opener after either sets a word free. `@` and `/` end a word
//! without expanding it. See `docs/features/shortcut-glued-words.md`.

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use crate::support::app_text_in;

const TABLE: &[(&str, &str)] = &[
    ("vn", "Việt Nam"),
    ("k", "không"),
    ("com", "công"),
    ("t", "tôi"),
    ("ct", "cơm tấm"),
];

fn engine() -> Engine {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    for (trigger, expansion) in TABLE {
        engine.add_shortcut(*trigger, *expansion);
    }
    engine
}

fn typed(keys: &str) -> String {
    app_text_in(&mut engine(), keys)
}

#[test]
fn a_word_glued_to_text_in_front_stays_as_typed() {
    for keys in [
        "abc.vn ",
        "google.com.vn ",
        "a@b.vn ",
        "x/vn ",
        "a_vn ",
        "#vn ",
        "-vn ",
        "abc,vn ",
        "ok.k ",
        "5.k ",
        "500k ",
        "don't ",
        "abc(vn ",
        "có/k ",
    ] {
        assert_eq!(typed(keys), keys, "{keys:?}");
    }
}

#[test]
fn a_word_standing_alone_still_expands() {
    for (keys, text) in [
        ("vn ", "Việt Nam "),
        ("abc vn ", "abc Việt Nam "),
        ("abc. vn ", "abc. Việt Nam "),
        ("(vn) ", "(Việt Nam) "),
        ("'vn' ", "'Việt Nam' "),
        ("\"vn\" ", "\"Việt Nam\" "),
        ("vn.", "Việt Nam."),
        ("vn,", "Việt Nam,"),
        ("ct)", "cơm tấm)"),
    ] {
        assert_eq!(typed(keys), text, "{keys:?}");
    }
}

#[test]
fn at_and_slash_end_a_word_without_expanding_it() {
    for keys in ["ct@gmail.com ", "ct/abc ", "vn/"] {
        assert_eq!(typed(keys), keys, "{keys:?}");
    }
}

#[test]
fn english_mode_leaves_a_glued_word_alone_too() {
    let mut engine = engine();
    engine.update_config(|c| c.shortcuts_in_english = true);
    engine.set_enabled(false);
    assert_eq!(app_text_in(&mut engine, "abc.vn "), "abc.vn ");
    engine.clear();
    assert_eq!(app_text_in(&mut engine, "vn "), "Việt Nam ");
}

#[test]
fn enter_and_tab_leave_a_glued_word_alone() {
    let mut engine = engine();
    app_text_in(&mut engine, "tuoitre.vn");
    assert_eq!(engine.expand_shortcut(), None);
}

/// A moved caret says nothing about what is in front of it: the next word is free.
#[test]
fn a_moved_caret_frees_the_next_word() {
    let mut engine = engine();
    app_text_in(&mut engine, "abc.");
    engine.clear();
    engine.disarm_capitalization();
    assert_eq!(app_text_in(&mut engine, "vn "), "Việt Nam ");
}

/// The host resets the engine right after a committed boundary (Android): the
/// full stop still glues the next word.
#[test]
fn a_host_reset_after_a_boundary_keeps_the_word_glued() {
    let mut engine = engine();
    app_text_in(&mut engine, "abc.");
    engine.clear();
    assert_eq!(app_text_in(&mut engine, "vn "), "vn ");
}

/// Ctrl seals `ct`; what follows is glued to it, so `ct` Ctrl `.vn` and
/// `ct` Ctrl `k` stay as typed.
#[test]
fn a_word_typed_right_after_a_sealed_one_is_glued() {
    for rest in [".vn ", "k "] {
        let mut engine = engine();
        let mut text = app_text_in(&mut engine, "ct");
        assert!(engine.seal().is_some());
        text.push_str(&app_text_in(&mut engine, rest));
        assert_eq!(text, format!("ct{rest}"));
    }
}

/// Space typed on a word re-opened by Backspace: glued when the host saw text in
/// front, free when it saw whitespace or nothing it could read.
#[test]
fn a_reopened_word_follows_what_the_host_saw_in_front() {
    let space_after = |before| {
        let mut engine = engine();
        assert!(engine.adopt_after("vn", before));
        engine.process_char(' ').action
    };
    assert_eq!(space_after(Some('.')), Action::None);
    assert_eq!(space_after(Some(' ')), Action::Send);
    assert_eq!(space_after(None), Action::Send);

    let mut engine = engine();
    assert!(engine.adopt("vn"));
    assert_eq!(engine.process_char(' ').action, Action::Send);
}
