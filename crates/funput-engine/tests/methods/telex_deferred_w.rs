use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn resolves_with_minimal_diff_and_preserves_raw_keys() {
    let (buffer, results) = crate::support::type_keys(InputMethod::Telex, "lwa");
    assert_eq!(buffer, "lă");
    assert_eq!(results[2].action, Action::Send);
    assert_eq!(results[2].backspace, 1);
    assert_eq!(results[2].output, "ă");

    let mut engine = Engine::new();
    for key in "swan".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "săn");
    assert_eq!(engine.keys(), "swan");
}

#[test]
fn flip_is_the_sticky_latin_escape() {
    let mut engine = Engine::new();
    for key in "swan".chars() {
        engine.process_char(key);
    }
    engine.flip_composing();
    assert_eq!(engine.buffer(), "swan");
    let boundary = engine.process_char(' ');
    assert_eq!(boundary.action, Action::None);
    assert!(engine.buffer().is_empty());
}

#[test]
fn literal_and_boundary_cases_do_not_leak() {
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "was wow win swift "),
        "was wow win swift "
    );
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "lwwa "),
        "lwwa "
    );
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "lw a "),
        "lw a "
    );
}
