use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn pending_w_then_stroke_converges() {
    for (keys, output) in [
        ("dwduocj", "được"),
        ("dwduongf", "đường"),
        ("dwdon", "đơn"),
        ("dwdoif", "đời"),
    ] {
        assert_eq!(crate::support::app_text(InputMethod::Telex, keys), output);
    }
}

#[test]
fn latin_collisions_keep_current_engine_behavior() {
    for (keys, output) in [
        ("dad ", "đa "),
        ("did ", "đi "),
        ("dead ", "dead "),
        ("droid ", "droid "),
        ("dwdead ", "dwdead "),
        ("dwswift ", "dwswift "),
    ] {
        assert_eq!(crate::support::app_text(InputMethod::Telex, keys), output);
    }
}

#[test]
fn raw_keys_flip_and_sticky_latin_are_preserved() {
    let mut engine = Engine::new();
    for key in "dwduocj".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "được");
    assert_eq!(engine.keys(), "dwduocj");
    engine.flip_composing();
    assert_eq!(engine.buffer(), "dwduocj");
    engine.flip_composing();
    assert_eq!(engine.buffer(), "được");
    engine.flip_composing();
    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert!(engine.buffer().is_empty());
    assert!(engine.keys().is_empty());
}

#[test]
fn nested_resolution_sends_the_minimal_diff() {
    let (_, results) = crate::support::type_keys(InputMethod::Telex, "dwdu");
    assert_eq!(results[2].action, Action::Send);
    assert_eq!(results[2].backspace, 2);
    assert_eq!(results[2].output, "đw");
    assert_eq!(results[3].action, Action::Send);
    assert_eq!(results[3].backspace, 1);
    assert_eq!(results[3].output, "ư");
}

#[test]
fn backspace_syncs_each_displayed_pending_stage() {
    for (keys, before, after) in [("dw", "dw", "d"), ("dwd", "đw", "đ"), ("dwdu", "đư", "đ")] {
        let mut engine = Engine::new();
        for key in keys.chars() {
            engine.process_char(key);
        }
        assert_eq!(engine.buffer(), before);
        engine.on_backspace();
        assert_eq!(engine.buffer(), after);
        assert_eq!(engine.keys(), after);
    }
}

#[test]
fn incomplete_intent_restores_at_word_boundary() {
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "dwd a "),
        "dwd a "
    );
}
