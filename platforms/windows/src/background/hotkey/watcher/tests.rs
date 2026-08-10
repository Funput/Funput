use super::Event::OtherDown;
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

const CTRL: Mods = Mods {
    ctrl: true,
    alt: false,
    win: false,
    shift: false,
};
const ALT: Mods = Mods {
    ctrl: false,
    alt: true,
    win: false,
    shift: false,
};
const SHIFT: Mods = Mods {
    ctrl: false,
    alt: false,
    win: false,
    shift: true,
};

fn down(bit: Mods) -> Event {
    Event::Modifier { bit, down: true }
}

fn up(bit: Mods) -> Event {
    Event::Modifier { bit, down: false }
}

#[test]
fn bare_pair_fires_on_the_first_release() {
    let mut w = Watcher::default();
    assert_eq!(w.feed(down(ALT), mods(false, true, false, false)), None);
    assert_eq!(w.feed(down(SHIFT), mods(false, true, true, false)), None);
    // Shift comes up first: the pair is already known from the peak.
    assert_eq!(
        w.feed(up(SHIFT), mods(false, true, false, false)),
        Some(mods(false, true, true, false))
    );
    // Alt following it must not fire a second time.
    assert_eq!(w.feed(up(ALT), NONE), None);
}

#[test]
fn a_key_pressed_in_between_cancels_the_gesture() {
    // Alt+Shift+Tab belongs to the app, not to us.
    let mut w = Watcher::default();
    w.feed(down(ALT), mods(false, true, false, false));
    w.feed(down(SHIFT), mods(false, true, true, false));
    w.feed(OtherDown, mods(false, true, true, false));
    assert_eq!(w.feed(up(SHIFT), mods(false, true, false, false)), None);
    assert_eq!(w.feed(up(ALT), NONE), None);
}

#[test]
fn a_third_modifier_is_reported_so_the_caller_can_reject_it() {
    // Ctrl+Alt+Shift is a different gesture from Alt+Shift and must not match it.
    let mut w = Watcher::default();
    w.feed(down(ALT), mods(false, true, false, false));
    w.feed(down(SHIFT), mods(false, true, true, false));
    w.feed(down(CTRL), mods(true, true, true, false));
    assert_eq!(
        w.feed(up(CTRL), mods(true, true, false, false)),
        Some(mods(true, true, true, false))
    );
}

#[test]
fn typing_before_the_modifiers_does_not_poison_the_next_gesture() {
    let mut w = Watcher::default();
    w.feed(OtherDown, NONE); // a plain letter, no modifier held
    w.feed(down(ALT), mods(false, true, false, false));
    w.feed(down(SHIFT), mods(false, true, true, false));
    assert_eq!(
        w.feed(up(SHIFT), mods(false, true, false, false)),
        Some(mods(false, true, true, false))
    );
}

#[test]
fn each_gesture_is_independent() {
    let mut w = Watcher::default();
    for _ in 0..2 {
        w.feed(down(CTRL), mods(true, false, false, false));
        w.feed(down(SHIFT), mods(true, false, true, false));
        w.feed(up(CTRL), mods(false, false, true, false));
        assert_eq!(w.feed(up(SHIFT), NONE), None); // gesture already spent
    }
    // A third run still reports the pair, i.e. state was reset each time.
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(
        w.feed(up(CTRL), mods(false, false, true, false)),
        Some(mods(true, false, true, false))
    );
}

#[test]
fn tapping_the_second_modifier_again_toggles_again() {
    // Ctrl stays down while Shift is tapped over and over — one toggle per tap,
    // like holding Alt and tapping Shift through Windows' input languages.
    let mut w = Watcher::default();
    w.feed(down(CTRL), mods(true, false, false, false));
    for _ in 0..3 {
        w.feed(down(SHIFT), mods(true, false, true, false));
        assert_eq!(
            w.feed(up(SHIFT), mods(true, false, false, false)),
            Some(mods(true, false, true, false))
        );
    }
    // Ctrl finally coming up is not a fourth toggle.
    assert_eq!(w.feed(up(CTRL), NONE), None);
}

#[test]
fn a_repeat_tap_is_judged_on_what_is_held_now() {
    // Ctrl+Shift fires, then Alt joins and Shift is tapped again: that tap is
    // Ctrl+Alt+Shift, which the caller must be able to tell apart from the pair.
    let mut w = Watcher::default();
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    w.feed(up(SHIFT), mods(true, false, false, false));

    w.feed(down(ALT), mods(true, true, false, false));
    w.feed(down(SHIFT), mods(true, true, true, false));
    assert_eq!(
        w.feed(up(SHIFT), mods(true, true, false, false)),
        Some(mods(true, true, true, false))
    );
}

#[test]
fn an_interrupted_gesture_does_not_repeat() {
    // Ctrl+Shift+Right is a selection. Letting go of Shift and pressing it again
    // to carry on selecting must not be read as a toggle.
    let mut w = Watcher::default();
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    w.feed(OtherDown, mods(true, false, true, false)); // Right arrow
    assert_eq!(w.feed(up(SHIFT), mods(true, false, false, false)), None);

    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(w.feed(up(SHIFT), mods(true, false, false, false)), None);
    assert_eq!(w.feed(up(CTRL), NONE), None);
}

#[test]
fn a_lagging_snapshot_on_the_last_release_does_not_latch_the_gesture() {
    // Releasing Ctrl+Shift together: `GetAsyncKeyState` still reports Ctrl down
    // on Shift's keyup, one event behind. The gesture must end on its own event
    // stream anyway, or every later press is swallowed (the "toggle stops
    // working after a press or two" bug).
    let mut w = Watcher::default();
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(
        w.feed(up(CTRL), mods(true, false, true, false)), // stale: ctrl still set
        Some(mods(true, false, true, false))
    );
    assert_eq!(w.feed(up(SHIFT), mods(true, false, false, false)), None); // stale again

    // The next press must fire, exactly as the first one did.
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(
        w.feed(up(CTRL), mods(false, false, true, false)),
        Some(mods(true, false, true, false))
    );
}

#[test]
fn a_release_the_hook_never_saw_is_recovered_from() {
    // A keyup lost while an elevated window had focus leaves Shift stuck in the
    // tracker. The next press starts from a snapshot saying nothing is down, so
    // the gesture restarts clean instead of staying spent forever.
    let mut w = Watcher::default();
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    w.feed(up(CTRL), mods(false, false, true, false)); // fires; Shift's keyup is lost

    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(
        w.feed(up(CTRL), mods(false, false, true, false)),
        Some(mods(true, false, true, false))
    );
}

#[test]
fn a_modifier_already_held_when_the_gesture_starts_still_counts() {
    // Ctrl was down before the tracker saw anything (its keydown was lost). The
    // snapshot carries it, so Ctrl+Shift is reported rather than a bare Shift.
    let mut w = Watcher::default();
    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(
        w.feed(up(SHIFT), mods(true, false, false, false)),
        Some(mods(true, false, true, false))
    );
}

#[test]
fn a_click_ends_the_gesture_without_stranding_it() {
    // Ctrl+Shift+click is multi-select. The click cancels this gesture, and the
    // next press is unaffected.
    let mut w = Watcher::default();
    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    w.interrupt();
    assert_eq!(w.feed(up(CTRL), mods(false, false, true, false)), None);
    w.feed(up(SHIFT), NONE);

    w.feed(down(CTRL), mods(true, false, false, false));
    w.feed(down(SHIFT), mods(true, false, true, false));
    assert_eq!(
        w.feed(up(CTRL), mods(false, false, true, false)),
        Some(mods(true, false, true, false))
    );
}
