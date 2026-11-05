use super::*;
use crate::Action;
use funput_core::InputMethod;

#[test]
fn pending_appends_literal() {
    let mut session = Session::new();
    let result = process(&mut session, 'a', 'a', false);
    assert_eq!(result.action, Action::None);
    assert_eq!(session.buffer, "a");
}

#[test]
fn applied_tone_telex() {
    let mut session = Session::new();
    process(&mut session, 'a', 'a', false);
    let result = process(&mut session, 's', 's', false);
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
    let result = process(&mut session, '1', '1', false);
    assert_eq!(result.action, Action::None);
    assert_eq!(session.buffer, "ng1");
}

#[test]
fn eager_restore_on_dead_end() {
    let mut session = Session::new();
    for key in "tex".chars() {
        session.keys.push(key);
        process(&mut session, key, key, false);
    }
    assert_eq!(session.buffer, "tẽ");

    session.keys.push('t');
    let result = process(&mut session, 't', 't', false);
    assert_eq!(result.action, Action::Send);
    assert_eq!(session.buffer, "text");
}

fn typed(session: &mut Session, keys: &str) -> ImeResult {
    let mut last = ImeResult::none();
    for key in keys.chars() {
        session.keys.push(key);
        last = process(session, key, key, false);
    }
    last
}

#[test]
fn nang_after_eager_restore_recomposes() {
    // `hõc` dead-ends on the `c` and restores to `hoxc`; the nặng key re-tones the
    // abandoned composition, so the app rewrites the raw keys after the shared `h`.
    let mut session = Session::new();
    typed(&mut session, "hoxc");
    assert_eq!(session.buffer, "hoxc");
    assert_eq!(session.vn_form, "hõc");

    let result = typed(&mut session, "j");
    assert_eq!(result.action, Action::Send);
    assert_eq!(result.backspace, 3);
    assert_eq!(result.output, "ọc");
    assert_eq!(session.buffer, "học");
    assert_eq!(session.vn_form, "học");
    assert_eq!(session.keys, "hoxcj", "keys stay the raw keystrokes");
}

#[test]
fn sac_after_eager_restore_stays_raw() {
    // Only nặng rescues: a sắc key after a restore is far more often English
    // (`parts`, `texts`) than a tone slip, so it appends to the raw keys.
    let mut session = Session::new();
    let result = typed(&mut session, "hofcs");
    assert_eq!(result.action, Action::None);
    assert_eq!(session.buffer, "hofcs");
}

#[test]
fn shadow_keeps_ch_coda_vietnamese() {
    // The `h` of a `ch` coda appends to the raw keys on screen but extends the
    // abandoned composition, so the nặng key after it still finds `sãch` to re-tone.
    let mut session = Session::new();
    let result = typed(&mut session, "saxch");
    assert_eq!(result.action, Action::None);
    assert_eq!(session.buffer, "saxch");
    assert_eq!(session.vn_form, "sãch");

    let result = typed(&mut session, "j");
    assert_eq!((result.backspace, result.output.as_str()), (4, "ạch"));
    assert_eq!(session.buffer, "sạch");
}
