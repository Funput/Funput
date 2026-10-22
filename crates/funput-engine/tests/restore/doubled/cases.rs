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
fn digits_glued_to_a_listed_word_keep_its_keys() {
    for (keys, text) in [
        ("pass1 ", "pass1 "),
        ("boss23 ", "boss23 "),
        ("Password1 ", "Password1 "),
        // `off` is not listed, so the habit still wins with a number after it.
        ("off1 ", "of1 "),
    ] {
        assert_eq!(telex(keys), text, "{keys:?}");
    }
}

/// With eager restore off the doubled key lands on a composed tone (`clá` + `s`)
/// rather than on raw letters, so many more words revert. The table lists those too.
#[test]
fn eager_restore_off_gets_back_the_words_only_it_shortens() {
    for (keys, text) in [
        ("class ", "class "),
        ("order ", "order "),
        ("process ", "process "),
        ("cases ", "cases "),
        ("received ", "received "),
    ] {
        let mut on = engine(InputMethod::Telex, true);
        on.update_config(|config| config.eager_restore = false);
        assert_eq!(app_text_in(&mut on, keys), text, "{keys:?}");
        let mut off = engine(InputMethod::Telex, false);
        off.update_config(|config| config.eager_restore = false);
        assert_ne!(
            app_text_in(&mut off, keys),
            text,
            "{keys:?} needs no restore"
        );
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
    // `w` alone is `ư` there, so `ww` is how a plain `w` is typed.
    let mut advanced = engine(InputMethod::TelexAdvanced, true);
    assert_eq!(app_text_in(&mut advanced, "ww "), "w ");
}

/// A revert can lose a key that sits apart from its twin (`ma[r]ke[r]s`). Nobody types
/// that for the shorter word, so it comes back even when the shorter one is commoner.
#[test]
fn a_key_lost_away_from_its_twin_comes_back() {
    for (keys, text) in [
        ("casts ", "casts "),
        ("nests ", "nests "),
        ("doses ", "doses "),
    ] {
        assert_eq!(telex(keys), text, "{keys:?}");
    }
    let mut lazy = engine(InputMethod::Telex, true);
    lazy.update_config(|config| config.eager_restore = false);
    assert_eq!(app_text_in(&mut lazy, "markers "), "markers ");
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
