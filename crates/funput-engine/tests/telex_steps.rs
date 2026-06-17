mod support;

use funput_core::InputMethod;
use funput_engine::Action;

#[test]
fn telex_as_step_by_step() {
    let mut engine = funput_engine::Engine::new();
    let r1 = engine.process_char('a');
    assert_eq!(r1.action, Action::None);
    assert_eq!(r1.backspace, 0);
    assert!(r1.output.is_empty());
    assert_eq!(engine.buffer(), "a");

    let r2 = engine.process_char('s');
    assert_eq!(r2.action, Action::Send);
    assert_eq!(r2.backspace, 1);
    assert_eq!(r2.output, "á");
    assert_eq!(engine.buffer(), "á");
    assert_eq!(engine.keys(), "as");
}

#[test]
fn telex_dd_stroke() {
    let (buffer, results) = support::type_keys(InputMethod::Telex, "dd");
    assert_eq!(buffer, "đ");
    assert_eq!(results[0].action, Action::None);
    assert_eq!(results[1].action, Action::Send);
    assert_eq!(results[1].backspace, 1);
    assert_eq!(results[1].output, "đ");
}

#[test]
fn telex_ass_revert_tone() {
    // Double tone key restores raw keystrokes: "á" + "s" → "as".
    let (buffer, results) = support::type_keys(InputMethod::Telex, "ass");
    assert_eq!(buffer, "as");
    assert_eq!(results.len(), 3);
    assert_eq!(results[0].action, Action::None);
    assert_eq!(results[2].action, Action::Send);
    assert_eq!(results[2].output, "as");
}

#[test]
fn telex_ngs_literal() {
    let (buffer, results) = support::type_keys(InputMethod::Telex, "ngs");
    assert_eq!(buffer, "ngs");
    assert_eq!(results[2].action, Action::None);
    assert_eq!(results[2].backspace, 0);
    assert!(results[2].output.is_empty());
}

#[test]
fn telex_truowng_complex() {
    assert_eq!(
        support::type_keys_buffer(InputMethod::Telex, "truowng"),
        "trương"
    );
}
