use super::*;
use crate::Action;
use funput_core::InputMethod;

#[test]
fn pending_appends_literal() {
    let mut session = Session::new();
    let result = process(&mut session, 'a', false);
    assert_eq!(result.action, Action::None);
    assert_eq!(session.buffer, "a");
}

#[test]
fn applied_tone_telex() {
    let mut session = Session::new();
    process(&mut session, 'a', false);
    let result = process(&mut session, 's', false);
    assert_eq!(result.action, Action::Send);
    assert_eq!(result.backspace, 1);
    assert_eq!(result.output, "á");
    assert_eq!(session.buffer, "á");
}

#[test]
fn ignored_appends_literal_vni() {
    let mut session = Session::new();
    session.config.method = InputMethod::Vni;
    session.buffer.push_str("ng");
    session.keys.push_str("ng1");
    let result = process(&mut session, '1', false);
    assert_eq!(result.action, Action::None);
    assert_eq!(session.buffer, "ng1");
}

#[test]
fn eager_restore_on_dead_end() {
    let mut session = Session::new();
    for key in "tex".chars() {
        session.keys.push(key);
        process(&mut session, key, false);
    }
    assert_eq!(session.buffer, "tẽ");

    session.keys.push('t');
    let result = process(&mut session, 't', false);
    assert_eq!(result.action, Action::Send);
    assert_eq!(session.buffer, "text");
}
