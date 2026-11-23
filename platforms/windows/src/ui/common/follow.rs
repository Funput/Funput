//! Keep a window's VI/EN switch on what the hook process last saved.
//!
//! A hotkey flips VI/EN in the background process and writes it to
//! `settings.json`; a window process read that file once, when it opened. A timer
//! compares the file's stamp and, once another process has saved, takes the
//! hook's half of the file.

use std::cell::Cell;
use std::time::Duration;

use slint::{ComponentHandle, Timer, TimerMode};

use crate::shared::{settings_path, shell};

/// Same pace as the tray's watch in the background process.
const INTERVAL: Duration = Duration::from_millis(250);

/// Call `show` with the new state each time the hook's VI/EN changes, for as long
/// as the returned timer lives. Hold it across the event loop.
///
/// The file is read with `refresh_hook_state`, which takes only VI/EN and the app
/// pins: the Settings window keeps gõ tắt drafts that are deliberately not on
/// disk, and `reload_settings` would replace them with the file. A save made by
/// this window changes the stamp too, but not the state, so it redraws nothing.
///
/// The timer holds a weak handle and never keeps the window alive.
#[must_use = "the window stops following VI/EN when the timer is dropped"]
pub(crate) fn follow_vi_en<C>(component: &C, show: fn(&C, bool)) -> Timer
where
    C: ComponentHandle + 'static,
{
    let weak = component.as_weak();
    let seen = Cell::new(settings_path::stamp());
    let timer = Timer::default();
    timer.start(TimerMode::Repeated, INTERVAL, move || {
        let now = settings_path::stamp();
        if seen.replace(now) == now {
            return;
        }
        let Some(component) = weak.upgrade() else {
            return;
        };
        let before = shell::enabled();
        shell::refresh_hook_state();
        let after = shell::enabled();
        if after != before {
            show(&component, after);
        }
    });
    timer
}
