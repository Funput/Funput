mod support;

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn telex_valid_vn_keeps_composed_on_space() {
    assert_eq!(support::app_text(InputMethod::Telex, "mas "), "má ");
}

#[test]
fn telex_english_restore_on_space() {
    assert_eq!(support::app_text(InputMethod::Telex, "absc "), "absc ");
}

#[test]
fn telex_pass_through_english_no_restore() {
    assert_eq!(support::app_text(InputMethod::Telex, "file "), "file ");
}

#[test]
fn telex_english_restore_on_punctuation() {
    assert_eq!(support::app_text(InputMethod::Telex, "absc,"), "absc,");
}

#[test]
fn revert_then_space_keeps_reverted_word() {
    // "mix" → "mĩ"; double `x` reverts to "mix"; pressing Space must NOT re-restore
    // the stale raw keystrokes ("mixx"). The revert is the user's final intent.
    assert_eq!(support::app_text(InputMethod::Telex, "mixx "), "mix ");
    assert_eq!(support::app_text(InputMethod::Vni, "a11 "), "a1 ");
}

#[test]
fn telex_eager_restore_step() {
    // "tẽ" is still valid Vietnamese, so it composes; the closing "t" makes
    // "tẽt" a dead end, restoring "text" the instant it is typed — no boundary.
    let mut engine = Engine::new();
    for key in "tex".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "tẽ");

    let closing = engine.process_char('t');
    assert_eq!(closing.action, Action::Send);
    assert_eq!(engine.buffer(), "text");
    assert_eq!(engine.keys(), "text");
}

#[test]
fn telex_mas_space_no_restore() {
    let mut engine = Engine::new();
    engine.process_char('m');
    engine.process_char('a');
    engine.process_char('s');
    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert!(space.output.is_empty());
}
