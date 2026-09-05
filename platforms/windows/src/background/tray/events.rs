//! Tray click and context-menu event handling.
//!
//! Windows delivers one double-click on a notification icon as *four* events —
//! `Click{Down}`, `Click{Up}`, `DoubleClick`, `Click{Up}` — so the single-click
//! action has already run by the time the double-click arrives, and there is no
//! way to see it coming. Deferring every single click by `GetDoubleClickTime()`
//! would put half a second between the tray and the Control Center, which is the
//! gesture people use most, so the single click stays instant and the *trailing*
//! `Click{Up}` is dropped instead.
//!
//! Dropping it is not optional. Left in, it re-enters `toggle_control_center`,
//! where `is_cc` is now false (the slot holds Settings) and the
//! `LAST_CC_DISMISS_MS` debounce is unarmed — that timestamp is only written when
//! the flyout exits with `EXIT_DISMISS`, and here it was killed. So it would fall
//! straight through to killing Settings and reopening the flyout: the exact
//! opposite of what the double-click asked for.

use std::cell::Cell;
use std::time::{Duration, Instant};

use tray_icon::menu::MenuEvent;
use tray_icon::{MouseButton, MouseButtonState, Rect, TrayIconEvent};
use windows::Win32::UI::Input::KeyboardAndMouse::GetDoubleClickTime;

use super::menu::{CONVERT_ID, QUIT_ID, SETTINGS_ID, UPDATE_ID};
use crate::background::hook;
use crate::ui;

thread_local! {
    /// When the last tray double-click landed. `thread_local` for the same reason
    /// the tray itself is: only the hook thread ever drains these events.
    ///
    /// A timestamp rather than a "skip the next Up" flag on purpose — a flag that
    /// never gets consumed stays armed and silently eats the next *legitimate*
    /// click, breaking the primary gesture. This expires on its own.
    static LAST_DBLCLK: Cell<Option<Instant>> = const { Cell::new(None) };
}

/// The two left-button tray events worth acting on.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum LeftEvent {
    Up,
    Double,
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Action {
    ControlCenter,
    Settings,
    Ignore,
}

/// Drain pending tray + menu events. Call after each `DispatchMessageW`.
///
/// Child reaping is deliberately not done here: the pump waits on the child's
/// handle directly, so polling it once per dispatched message would only add a
/// `try_wait` syscall to every mouse move the low-level hook delivers.
pub(super) fn drain() {
    while let Ok(ev) = TrayIconEvent::receiver().try_recv() {
        let Some((left, rect)) = as_left_event(&ev) else {
            continue;
        };
        let window = Duration::from_millis(u64::from(unsafe { GetDoubleClickTime() }));
        let now = Instant::now();
        match decide(left, LAST_DBLCLK.get(), now, window) {
            // Toggle VI lives on the flyout (the hotkey still refreshes the tray
            // glyph via `ON_TOGGLE`).
            Action::ControlCenter => ui::toggle_control_center(rect),
            Action::Settings => {
                LAST_DBLCLK.set(Some(now));
                // Kills the flyout the opening click just spawned, then opens the
                // Settings window on its overview tab.
                ui::launch_settings(false);
            }
            Action::Ignore => {}
        }
    }

    while let Ok(ev) = MenuEvent::receiver().try_recv() {
        handle_menu(ev.id.0.as_str());
    }
}

fn as_left_event(ev: &TrayIconEvent) -> Option<(LeftEvent, Rect)> {
    match *ev {
        TrayIconEvent::Click {
            button: MouseButton::Left,
            button_state: MouseButtonState::Up,
            rect,
            ..
        } => Some((LeftEvent::Up, rect)),
        // Note this variant carries no `button_state` — it is emitted once, for
        // the `WM_LBUTTONDBLCLK` in the middle of the four-event sequence.
        TrayIconEvent::DoubleClick {
            button: MouseButton::Left,
            rect,
            ..
        } => Some((LeftEvent::Double, rect)),
        _ => None,
    }
}

/// What a left-button tray event should do, given when the last double-click
/// landed.
///
/// Pure so the four-event sequence Windows sends for one double-click can be
/// replayed in a test without a mouse or a clock. The `window` guard swallows the
/// trailing `Up`, and a second `Double` with it, so a triple-click does not kill
/// and respawn the Settings window.
fn decide(ev: LeftEvent, last: Option<Instant>, now: Instant, window: Duration) -> Action {
    if last.is_some_and(|t| now.saturating_duration_since(t) <= window) {
        return Action::Ignore;
    }
    match ev {
        LeftEvent::Up => Action::ControlCenter,
        LeftEvent::Double => Action::Settings,
    }
}

fn handle_menu(id: &str) {
    match id {
        SETTINGS_ID => ui::launch_settings(false),
        CONVERT_ID => ui::launch_convert(),
        UPDATE_ID => ui::launch_settings(true),
        QUIT_ID => {
            ui::terminate_children();
            hook::quit();
        }
        _ => {}
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A typical `GetDoubleClickTime()`.
    fn window() -> Duration {
        Duration::from_millis(500)
    }

    #[test]
    fn it_opens_the_control_center_on_a_lone_click() {
        let action = decide(LeftEvent::Up, None, Instant::now(), window());
        assert_eq!(action, Action::ControlCenter);
    }

    #[test]
    fn it_opens_settings_on_a_double_click() {
        let action = decide(LeftEvent::Double, None, Instant::now(), window());
        assert_eq!(action, Action::Settings);
    }

    #[test]
    fn the_trailing_up_of_a_double_click_does_nothing() {
        // The reported bug, replayed as Windows actually delivers it. Without the
        // last assertion that trailing Up re-enters toggle_control_center, kills
        // the Settings window that just opened, and reopens the flyout instead.
        let t0 = Instant::now();
        assert_eq!(
            decide(LeftEvent::Up, None, t0, window()),
            Action::ControlCenter
        );

        let t1 = t0 + Duration::from_millis(120);
        assert_eq!(
            decide(LeftEvent::Double, None, t1, window()),
            Action::Settings
        );

        let t2 = t1 + Duration::from_millis(2);
        assert_eq!(
            decide(LeftEvent::Up, Some(t1), t2, window()),
            Action::Ignore
        );
    }

    #[test]
    fn a_triple_click_does_not_relaunch_settings() {
        let t = Instant::now();
        let third = decide(
            LeftEvent::Double,
            Some(t),
            t + Duration::from_millis(150),
            window(),
        );
        assert_eq!(third, Action::Ignore);
    }

    #[test]
    fn a_click_after_the_window_works_again() {
        // The suppression expires on its own, so a stuck state cannot swallow the
        // gesture the tray is used for most.
        let t = Instant::now();
        let later = t + window() + Duration::from_millis(1);
        assert_eq!(
            decide(LeftEvent::Up, Some(t), later, window()),
            Action::ControlCenter
        );
    }

    #[test]
    fn the_window_boundary_still_suppresses() {
        let t = Instant::now();
        let edge = t + window();
        assert_eq!(
            decide(LeftEvent::Up, Some(t), edge, window()),
            Action::Ignore
        );
    }
}
