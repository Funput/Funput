//! The mid-word switch: once the shortened form on screen can no longer begin an
//! English word, the raw keys come back at once instead of at the word boundary.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules};
use funput_engine::{Action, Engine, ImeResult};

use super::engine;
use crate::support::app_text_in;

const WORDS: &str = include_str!("../../../data/doubled/words.txt");

fn typed(engine: &mut Engine, keys: &str) {
    for key in keys.chars() {
        engine.process_char(key);
    }
}

#[test]
fn the_raw_keys_come_back_on_the_decisive_key() {
    let mut on = engine(InputMethod::Telex, true);
    typed(&mut on, "pass");
    assert_eq!(on.buffer(), "pas");
    assert_eq!(
        on.process_char('w'),
        ImeResult {
            action: Action::Send,
            backspace: 0,
            output: "sw".into(),
        }
    );
    assert_eq!(on.buffer(), "passw");
    assert_eq!(on.keys(), "passw");
    typed(&mut on, "ord");
    assert_eq!(on.process_char(' ').action, Action::None);
}

#[test]
fn each_word_switches_where_the_generator_found_it_decided() {
    for (word, decisive) in [
        ("password", "passw"),
        ("correct", "correc"),
        ("office", "offi"),
        ("different", "diffe"),
        ("session", "sessi"),
        ("Password", "Passw"),
        // Decisive on the reverting key itself: `bái` + `s` shows `basis` at once.
        ("basis", "basis"),
    ] {
        let mut on = engine(InputMethod::Telex, true);
        typed(&mut on, &decisive[..decisive.len() - 1]);
        assert_ne!(
            on.buffer(),
            &decisive[..decisive.len() - 1],
            "{word} switched early"
        );
        typed(&mut on, &decisive[decisive.len() - 1..]);
        assert_eq!(on.buffer(), decisive, "{word}");
    }
}

#[test]
fn the_doubled_key_habit_never_switches_mid_word() {
    for keys in [
        "tesst ", "lasst ", "iss ", "mixx ", "off ", "offf ", "hiss ",
    ] {
        let mut on = engine(InputMethod::Telex, true);
        let mut off = engine(InputMethod::Telex, false);
        for key in keys.chars() {
            assert_eq!(
                on.process_char(key),
                off.process_char(key),
                "{keys:?} at {key:?}"
            );
        }
    }
}

#[test]
fn eager_restore_off_waits_for_the_boundary() {
    let mut waiting = engine(InputMethod::Telex, true);
    waiting.update_config(|config| config.eager_restore = false);
    typed(&mut waiting, "passw");
    assert_ne!(waiting.buffer(), "passw");
    assert_eq!(app_text_in(&mut waiting, "ord "), "password ");
}

#[test]
fn flip_after_the_switch_toggles_like_any_restored_word() {
    let mut on = engine(InputMethod::Telex, true);
    typed(&mut on, "passw");
    on.flip_composing();
    assert_eq!(on.buffer(), "păs");
    on.flip_composing();
    assert_eq!(on.buffer(), "passw");
    typed(&mut on, "ord");
    assert_eq!(on.buffer(), "password");
}

/// The mid-word switch only moves the moment a listed word is decided, never the
/// word it ends up as: with eager restore on or off, every listed word in every
/// setup the list was built from commits the same text.
#[test]
fn every_listed_word_ends_the_same_switched_mid_word_or_at_the_boundary() {
    let wide = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
    for method in [InputMethod::Telex, InputMethod::TelexAdvanced] {
        for rules in [SyllableRules::STANDARD, wide] {
            let text = |eager: bool, word: &str| {
                let mut engine = engine(method, true);
                engine.update_config(|config| {
                    config.syllable_rules = rules;
                    config.eager_restore = eager;
                });
                app_text_in(&mut engine, &format!("{word} "))
            };
            for word in WORDS.lines() {
                assert_eq!(
                    text(true, word),
                    text(false, word),
                    "{word} under {method:?}"
                );
            }
        }
    }
}
