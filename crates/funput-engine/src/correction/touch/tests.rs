//! The touch log's own rules: what it records, and when it stops trusting itself.

use super::*;
use crate::correction::state;
use crate::model::Session;

fn session() -> Session {
    let mut session = Session::new();
    session.config.typo_correction = true;
    state::sync(&mut session);
    session
}

fn log(session: &Session) -> &TouchLog {
    &session.correction.as_ref().expect("state is on").touch
}

/// Feed one key the way `Engine::process_key` does: the raw key, plus the touch the
/// host reported for it.
fn press(session: &mut Session, key: char, reported: char) {
    set_next(
        session,
        KeyTouch::new(reported, 0.25).with_alternate('x', 0.1),
    );
    let touch = take_next(session);
    session.keys.push(key);
    note_key(session, key, touch);
}

#[test]
fn the_log_lines_up_with_the_raw_keys_it_was_fed() {
    let mut session = session();
    for key in "nha".chars() {
        press(&mut session, key, key);
    }
    assert!(log(&session).matches("nha"));
    assert_eq!(log(&session).len(), 3);
}

#[test]
fn a_word_longer_than_the_key_cap_leaves_the_log_unusable() {
    let mut session = session();
    let keys = "abcdefghijk"; // MAX_WORD_KEYS + 1
    for key in keys.chars() {
        press(&mut session, key, key);
    }
    assert!(!log(&session).matches(keys));
}

#[test]
fn an_uppercase_key_uppercases_the_alternates_offered_for_it() {
    // The host reports key caps; the engine records what it actually composed with,
    // so replaying an alternate has to keep the capital.
    let mut session = session();
    press(&mut session, 'N', 'n');
    let entry = log(&session).entries()[0];
    assert_eq!(entry.typed, 'N');
    assert_eq!(entry.alternates()[0].key, 'X');
}

#[test]
fn a_touch_for_a_key_the_engine_did_not_record_is_refused() {
    let mut session = session();
    press(&mut session, 'n', 'b');
    assert!(!log(&session).matches("n"));
}

#[test]
fn rewriting_the_raw_keys_under_the_log_makes_it_unusable() {
    let mut session = session();
    for key in "nha".chars() {
        press(&mut session, key, key);
    }
    session.keys.pop(); // what a reverted modifier does inside the pipeline
    verify_alignment(&mut session);
    assert!(!log(&session).matches("nh"));
}

#[test]
fn clearing_the_word_gives_the_log_a_fresh_start() {
    let mut session = session();
    press(&mut session, 'n', 'b'); // spoils the log
    session.clear();
    press(&mut session, 'n', 'n');
    assert!(log(&session).matches("n"));
}

#[test]
fn a_touch_is_consumed_by_the_key_that_follows_it() {
    let mut session = session();
    set_next(&mut session, KeyTouch::new('a', 0.1));
    assert!(take_next(&mut session).is_some());
    assert!(take_next(&mut session).is_none());
}

#[test]
fn quantising_a_distance_round_trips_within_half_a_step() {
    for distance in [0.0_f32, 0.15, 0.5, 1.75, 3.0] {
        assert!((from_q8(to_q8(distance)) - distance).abs() < 1.0 / 512.0);
    }
    assert_eq!(
        to_q8(-1.0),
        0,
        "a nonsense distance clamps instead of wrapping"
    );
    assert_eq!(to_q8(f32::NAN), 0);
}
