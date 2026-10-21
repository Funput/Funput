use funput_core::InputMethod;
use funput_engine::{Action, ImeResult};

use super::{engine, telex};
use crate::support::app_text_in;

#[test]
fn an_english_word_gets_its_doubled_key_back() {
    for (keys, text) in [
        ("password ", "password "),
        ("correct ", "correct "),
        ("pass ", "pass "),
        ("boss ", "boss "),
        ("office ", "office "),
        ("coffee ", "coffee "),
        ("issue ", "issue "),
        ("seemed ", "seemed "),
        ("pass, ", "pass, "),
    ] {
        assert_eq!(telex(keys), text, "{keys:?}");
    }
}

#[test]
fn the_raw_keys_come_back_in_the_case_they_were_typed() {
    assert_eq!(telex("Password "), "Password ");
    assert_eq!(telex("PASSWORD "), "PASSWORD ");
}

#[test]
fn the_doubled_key_habit_still_gives_a_plain_letter() {
    for (keys, text) in [
        ("mixx ", "mix "),
        ("tesst ", "test "),
        ("lasst ", "last "),
        ("iss ", "is "),
        ("off ", "of "),
        ("offf ", "off "),
        ("passte ", "paste "),
    ] {
        assert_eq!(telex(keys), text, "{keys:?}");
    }
}

#[test]
fn vietnamese_is_untouched() {
    assert_eq!(telex("mas xins chaof ddaayf ddur "), "má xín chào đầy đủ ");
    let mut vni = engine(InputMethod::Vni, true);
    assert_eq!(app_text_in(&mut vni, "a11 "), "a1 ");
}

#[test]
fn advanced_telex_restores_too() {
    let mut advanced = engine(InputMethod::TelexAdvanced, true);
    assert_eq!(app_text_in(&mut advanced, "password "), "password ");
}

/// `s` is the onset and the sắc key at once: the first `ss` reverts the tone the
/// second `s` placed, so `session` used to lose an `s` and come out `sesion`.
#[test]
fn session_keeps_both_esses() {
    for (keys, text) in [
        ("session ", "session "),
        ("sessions ", "sessions "),
        ("Session ", "Session "),
        ("SESSION ", "SESSION "),
        ("session.", "session."),
    ] {
        assert_eq!(telex(keys), text, "{keys:?}");
    }

    assert_boundary_edit("session");
    let mut off = engine(InputMethod::Telex, false);
    assert_eq!(app_text_in(&mut off, "session "), "sesion ");
}

#[test]
fn the_boundary_replaces_the_shortened_word_in_one_edit() {
    assert_boundary_edit("password");
    assert_boundary_edit("pass");
}

/// With eager restore off nothing is decided mid-word, so `word` waits for its
/// boundary, which replaces whatever it shows with the raw keys in one edit.
fn assert_boundary_edit(word: &str) {
    let mut engine = engine(InputMethod::Telex, true);
    engine.update_config(|config| config.eager_restore = false);
    for key in word.chars() {
        engine.process_char(key);
    }
    assert_ne!(
        engine.buffer(),
        word,
        "{word} was decided before its boundary"
    );
    let shown = engine.buffer().chars().count();
    assert_eq!(
        engine.process_char(' '),
        ImeResult {
            action: Action::Send,
            backspace: shown,
            output: format!("{word} "),
        },
        "{word}"
    );
    assert_eq!(engine.buffer(), "");
}
