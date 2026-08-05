use super::Event::{ModifierDown, ModifierUp, OtherDown};
use super::*;

fn mods(ctrl: bool, alt: bool, shift: bool, win: bool) -> Mods {
    Mods {
        ctrl,
        alt,
        win,
        shift,
    }
}

const NONE: Mods = Mods {
    ctrl: false,
    alt: false,
    win: false,
    shift: false,
};

#[test]
fn bare_pair_fires_on_the_first_release() {
    let mut w = Watcher::default();
    assert_eq!(w.feed(ModifierDown, mods(false, true, false, false)), None);
    assert_eq!(w.feed(ModifierDown, mods(false, true, true, false)), None);
    // Shift comes up first: the pair is already known from the peak.
    assert_eq!(
        w.feed(ModifierUp, mods(false, true, false, false)),
        Some(mods(false, true, true, false))
    );
    // Alt following it must not fire a second time.
    assert_eq!(w.feed(ModifierUp, NONE), None);
}

#[test]
fn a_key_pressed_in_between_cancels_the_gesture() {
    // Alt+Shift+Tab belongs to the app, not to us.
    let mut w = Watcher::default();
    w.feed(ModifierDown, mods(false, true, false, false));
    w.feed(ModifierDown, mods(false, true, true, false));
    w.feed(OtherDown, mods(false, true, true, false));
    assert_eq!(w.feed(ModifierUp, mods(false, true, false, false)), None);
    assert_eq!(w.feed(ModifierUp, NONE), None);
}

#[test]
fn a_third_modifier_is_reported_so_the_caller_can_reject_it() {
    // Ctrl+Alt+Shift is a different gesture from Alt+Shift and must not match it.
    let mut w = Watcher::default();
    w.feed(ModifierDown, mods(false, true, false, false));
    w.feed(ModifierDown, mods(false, true, true, false));
    w.feed(ModifierDown, mods(true, true, true, false));
    assert_eq!(
        w.feed(ModifierUp, mods(true, true, false, false)),
        Some(mods(true, true, true, false))
    );
}

#[test]
fn typing_before_the_modifiers_does_not_poison_the_next_gesture() {
    let mut w = Watcher::default();
    w.feed(OtherDown, NONE); // a plain letter, no modifier held
    w.feed(ModifierDown, mods(false, true, false, false));
    w.feed(ModifierDown, mods(false, true, true, false));
    assert_eq!(
        w.feed(ModifierUp, mods(false, true, false, false)),
        Some(mods(false, true, true, false))
    );
}

#[test]
fn each_gesture_is_independent() {
    let mut w = Watcher::default();
    for _ in 0..2 {
        w.feed(ModifierDown, mods(true, false, false, false));
        w.feed(ModifierDown, mods(true, false, true, false));
        w.feed(ModifierUp, mods(true, false, false, false));
        assert_eq!(w.feed(ModifierUp, NONE), None); // gesture already spent
    }
    // A third run still reports the pair, i.e. state was reset each time.
    w.feed(ModifierDown, mods(true, false, false, false));
    w.feed(ModifierDown, mods(true, false, true, false));
    assert_eq!(
        w.feed(ModifierUp, mods(true, false, false, false)),
        Some(mods(true, false, true, false))
    );
}
