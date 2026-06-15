mod support;

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn telex_mas_then_space() {
    let mut engine = Engine::new();
    engine.process_char('m');
    engine.process_char('a');
    let tone = engine.process_char('s');
    assert_eq!(tone.action, Action::Send);
    assert_eq!(engine.buffer(), "má");

    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert_eq!(space.backspace, 0);
    assert!(space.output.is_empty());
    assert_eq!(engine.buffer(), "");
    assert_eq!(engine.keys(), "");
}

#[test]
fn telex_multi_word() {
    assert_eq!(
        support::type_words(InputMethod::Telex, "xins chaof banj"),
        "xín chào bạn"
    );
}

#[test]
fn vni_multi_word() {
    assert_eq!(
        support::type_words(InputMethod::Vni, "xin1 chao2"),
        "xín chào"
    );
}

#[test]
fn type_words_leaves_buffer_empty_after_trailing_space() {
    let mut engine = Engine::new();
    for key in "mas ".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "");
}

/// Reconstruct the app text from the inject stream (None → append key,
/// Send → delete `backspace` chars then append `output`).
fn app_text(method: InputMethod, keys: &str) -> String {
    let mut engine = Engine::new();
    engine.set_method(method);
    let mut app = String::new();
    for key in keys.chars() {
        let r = engine.process_char(key);
        match r.action {
            Action::None => app.push(key),
            Action::Send => {
                for _ in 0..r.backspace {
                    app.pop();
                }
                app.push_str(&r.output);
            }
            Action::Restore => unreachable!("no restore in E2"),
        }
    }
    app
}

#[test]
fn punctuation_is_a_boundary_no_cross_syllable_bleed() {
    // The modifier in the second chunk must not reach back to the first syllable.
    assert_eq!(app_text(InputMethod::Telex, "as,af"), "á,à");
    assert_eq!(app_text(InputMethod::Vni, "a1.a2"), "á.à");
    // Buffer resets after the punctuation so the second word composes cleanly.
    assert_eq!(app_text(InputMethod::Telex, "anhf-em"), "ành-em");
}
