//! Gõ tắt triggers written in Vietnamese (`đc` → `được`).
//!
//! A trigger is written the way the user thinks of the word, which is rarely the
//! keys behind it: nobody saves `ddc` (Telex) or `d9c` (VNI) for `đc`. The matcher
//! tries the raw keys first, so tables written as keys keep their meaning, then the
//! word on screen. See `src/compose/boundary/shortcut.rs`.

use funput_core::InputMethod;
use funput_engine::{Action, Engine, ImeResult};

use crate::support::app_text_in;

fn engine_with(method: InputMethod, shortcuts: &[(&str, &str)]) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(method);
    for (trigger, expansion) in shortcuts {
        engine.add_shortcut(*trigger, *expansion);
    }
    engine
}

fn typed(method: InputMethod, shortcuts: &[(&str, &str)], keys: &str) -> String {
    app_text_in(&mut engine_with(method, shortcuts), keys)
}

const DC: &[(&str, &str)] = &[("đc", "được")];

#[test]
fn a_vietnamese_trigger_expands_whatever_the_input_method() {
    assert_eq!(typed(InputMethod::Telex, DC, "ddc "), "được ");
    assert_eq!(typed(InputMethod::TelexAdvanced, DC, "ddc "), "được ");
    assert_eq!(typed(InputMethod::Vni, DC, "d9c "), "được ");
}

#[test]
fn a_trigger_with_a_tone_mark_expands() {
    let shortcuts = &[("mấy", "mấy giờ")];
    assert_eq!(typed(InputMethod::Telex, shortcuts, "maays "), "mấy giờ ");
    assert_eq!(typed(InputMethod::Vni, shortcuts, "may61 "), "mấy giờ ");
}

#[test]
fn a_vietnamese_trigger_follows_smart_case() {
    assert_eq!(typed(InputMethod::Telex, DC, "DDc "), "Được ");
    assert_eq!(typed(InputMethod::Telex, DC, "DDC "), "ĐƯỢC ");
    assert_eq!(typed(InputMethod::Vni, DC, "D9c "), "Được ");
}

#[test]
fn smart_case_off_needs_the_exact_vietnamese_trigger() {
    let mut engine = engine_with(InputMethod::Telex, DC);
    engine.update_config(|config| config.shortcut_smart_case = false);
    assert_eq!(app_text_in(&mut engine, "ddc "), "được ");
    assert_eq!(app_text_in(&mut engine, "DDc "), "Đc ");
}

#[test]
fn the_keys_win_over_the_composed_word() {
    let shortcuts = &[("ddc", "keys"), ("đc", "composed")];
    assert_eq!(typed(InputMethod::Telex, shortcuts, "ddc "), "keys ");
}

#[test]
fn ascii_triggers_keep_working_beside_vietnamese_ones() {
    let shortcuts = &[("đc", "được"), ("ko", "không")];
    assert_eq!(
        typed(InputMethod::Telex, shortcuts, "ko ddc "),
        "không được "
    );
}

#[test]
fn punctuation_ends_a_vietnamese_trigger_too() {
    assert_eq!(typed(InputMethod::Telex, DC, "ddc,"), "được,");
}

#[test]
fn a_word_that_only_starts_like_the_trigger_stays_itself() {
    assert_eq!(
        typed(InputMethod::Telex, DC, "ddcs "),
        typed(InputMethod::Telex, &[], "ddcs ")
    );
}

#[test]
fn a_vietnamese_trigger_glued_to_a_number_stays_literal() {
    assert_eq!(
        typed(InputMethod::Telex, DC, "500ddc "),
        typed(InputMethod::Telex, &[], "500ddc ")
    );
}

#[test]
fn a_corrected_vietnamese_trigger_still_expands() {
    let mut engine = engine_with(InputMethod::Telex, DC);
    let mut app = String::new();
    type_into(&mut engine, &mut app, "ddcc");
    let result = engine.on_backspace();
    apply(&mut app, None, &result);
    type_into(&mut engine, &mut app, " ");
    assert_eq!(app, "được ");
}

#[test]
fn english_mode_matches_a_literally_typed_vietnamese_trigger() {
    // Nothing composes in English mode, so `ddc` stays keys; a host that sends `đ`
    // itself still reaches the trigger.
    let mut engine = engine_with(InputMethod::Telex, DC);
    engine.set_enabled(false);
    assert_eq!(app_text_in(&mut engine, "ddc "), "ddc ");
    assert_eq!(app_text_in(&mut engine, "đc "), "được ");
}

fn type_into(engine: &mut Engine, app: &mut String, keys: &str) {
    for key in keys.chars() {
        let result = engine.process_char(key);
        apply(app, Some(key), &result);
    }
}

/// Apply a result the way a host does; a `None` Backspace (`key` is `None`) deletes
/// one character.
fn apply(app: &mut String, key: Option<char>, result: &ImeResult) {
    match result.action {
        Action::None => match key {
            Some(key) => app.push(key),
            None => {
                app.pop();
            }
        },
        Action::Send => {
            for _ in 0..result.backspace {
                app.pop();
            }
            app.push_str(&result.output);
        }
        Action::Restore => unreachable!("Restore not implemented yet"),
    }
}
