//! What survives a word ending, and what does not.

use funput_core::InputMethod;

use super::*;
use crate::correction::park;

fn session() -> Session {
    let mut session = Session::new();
    session.config.typo_correction = true;
    sync(&mut session);
    session
}

#[test]
fn clearing_a_word_keeps_the_correction_parked_at_the_boundary() {
    // The boundary parks a correction and *then* clears the session; the platform
    // answers afterwards, so the parked state has to outlive the word it came from.
    let mut session = session();
    session.buffer.push_str("dduwowfnh");
    session.keys.push_str("dduwowfnh");
    let state = session.correction.take().expect("state is on");
    let mut state = state;
    park(&mut state, &session, ' ', false);
    session.correction = Some(state);

    session.clear();

    let state = session.correction.as_ref().expect("state is on");
    assert!(state.pending.is_some());
    assert_eq!(state.touch.len(), 0, "the touch log is per-word");
}

#[test]
fn the_scratch_session_never_carries_correction_state_of_its_own() {
    // The state owns a `Session` and a `Session` owns the state: the recursion has to
    // stop here, or every word boundary would build a tower of them.
    let session = session();
    let state = session.correction.as_ref().expect("state is on");
    assert!(state.scratch.correction.is_none());
}

#[test]
fn switching_the_setting_off_drops_the_state() {
    let mut session = session();
    assert!(session.correction.is_some());
    session.config.typo_correction = false;
    sync(&mut session);
    assert!(session.correction.is_none());
}

#[test]
fn answering_when_nothing_is_pending_changes_nothing() {
    let mut session = session();
    assert_eq!(apply(&mut session, Some(0)).action, crate::Action::None);
    assert_eq!(apply(&mut session, None).action, crate::Action::None);
    assert!(flush_pending(&mut session).is_none());
}

#[test]
fn a_session_without_the_setting_answers_every_call_harmlessly() {
    let mut session = Session::new();
    session.config.method = InputMethod::Telex;
    assert_eq!(apply(&mut session, Some(0)).action, crate::Action::None);
    assert!(take_undo(&mut session).is_none());
    discard(&mut session);
}
