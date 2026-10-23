use std::time::{Duration, Instant};

use funput_engine::KeySource;

use super::{AccessKeys, IDLE_TIMEOUT};
use crate::{Caret, KeyKind, Mods};

const ALT: Mods = Mods {
    ctrl: false,
    alt: true,
    win: false,
    shift: false,
};

fn mods(ctrl: bool, alt: bool, shift: bool, win: bool) -> Mods {
    Mods {
        ctrl,
        alt,
        win,
        shift,
    }
}

fn letter(c: char) -> KeyKind {
    KeyKind::Compose(c, KeySource::Standard)
}

/// A mode entered at `t0`, and that instant.
fn entered() -> (AccessKeys, Instant) {
    let t0 = Instant::now();
    let mut keys = AccessKeys::default();
    assert!(keys.on_modifier_tap(ALT, t0));
    (keys, t0)
}

#[test]
fn off_by_default_composes_everything() {
    let mut keys = AccessKeys::default();
    let now = Instant::now();
    assert!(!keys.is_active(now));
    assert!(!keys.bypasses(&letter('e'), now));
    assert!(!keys.bypasses(&KeyKind::Backspace, now));
}

#[test]
fn excel_paste_values_reaches_the_app_untouched() {
    // Alt, E, S, V: Telex would turn `es` into `é` if any of these were composed.
    let (mut keys, t0) = entered();
    for c in ['e', 's', 'v'] {
        assert!(keys.bypasses(&letter(c), t0), "{c} must bypass composition");
    }
    assert!(keys.is_active(t0));
}

#[test]
fn only_alt_alone_opens_the_menus() {
    let now = Instant::now();
    for tapped in [
        mods(false, false, false, false),
        mods(true, false, false, false),
        mods(false, false, true, false),
        mods(false, false, false, true),
        mods(false, true, true, false), // Alt+Shift switches input language
        mods(true, true, false, false), // AltGr
    ] {
        let mut keys = AccessKeys::default();
        assert!(!keys.on_modifier_tap(tapped, now), "{tapped:?}");
        assert!(!keys.is_active(now), "{tapped:?}");
    }
}

#[test]
fn second_alt_tap_closes_the_menus() {
    let (mut keys, t0) = entered();
    assert!(!keys.on_modifier_tap(ALT, t0));
    assert!(!keys.is_active(t0));
    assert!(!keys.bypasses(&letter('e'), t0));
}

#[test]
fn backspace_and_inert_keys_stay_in_the_mode() {
    let (mut keys, t0) = entered();
    assert!(keys.bypasses(&KeyKind::Backspace, t0)); // steps a KeyTip back
    assert!(keys.bypasses(&KeyKind::PassThrough, t0));
    assert!(keys.is_active(t0));
}

#[test]
fn flushing_keys_end_the_mode_and_still_flush() {
    for caret in [Caret::LineStart, Caret::Unknown] {
        let (mut keys, t0) = entered();
        assert!(!keys.bypasses(&KeyKind::Flush(caret), t0));
        assert!(!keys.is_active(t0));
        assert!(!keys.bypasses(&letter('s'), t0));
    }
}

#[test]
fn space_passes_through_and_ends_the_mode() {
    // An Alt tap that opened nothing, then `xin chaof`: only `xin` goes raw.
    let (mut keys, t0) = entered();
    assert!(keys.bypasses(&letter('x'), t0));
    assert!(keys.bypasses(&letter(' '), t0));
    assert!(!keys.is_active(t0));
    assert!(!keys.bypasses(&letter('c'), t0));
}

#[test]
fn idle_timeout_ends_the_mode() {
    let (mut keys, t0) = entered();
    let late = t0 + IDLE_TIMEOUT;
    assert!(!keys.is_active(late));
    assert!(!keys.bypasses(&letter('e'), late));
    // And it stays ended: a later key does not find a stale mode.
    assert!(!keys.is_active(t0));
}

#[test]
fn each_key_keeps_the_mode_alive() {
    // A slow multi-level ribbon sequence: every gap is under the timeout, the total is not.
    let (mut keys, t0) = entered();
    let step = IDLE_TIMEOUT - Duration::from_millis(500);
    let mut now = t0;
    for c in ['h', 'f', 'c'] {
        now += step;
        assert!(keys.bypasses(&letter(c), now), "{c} at {:?}", now - t0);
    }
}

#[test]
fn cancel_ends_the_mode() {
    let (mut keys, t0) = entered();
    keys.cancel();
    assert!(!keys.is_active(t0));
    assert!(!keys.bypasses(&letter('e'), t0));
}

#[test]
fn alt_tap_after_a_timeout_reopens_rather_than_closes() {
    // The mode lapsed unnoticed, so this tap is the user opening the menus again.
    let (mut keys, t0) = entered();
    let late = t0 + IDLE_TIMEOUT * 2;
    assert!(keys.on_modifier_tap(ALT, late));
    assert!(keys.is_active(late));
}
