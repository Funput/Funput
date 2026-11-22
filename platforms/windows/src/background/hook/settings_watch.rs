//! Refresh the tray while the Control Center is open.
//!
//! That flyout is its own process. It and the hook share `settings.json`, but the
//! hook only reloads the file when the child exits or focus returns to another
//! app — and while the panel is up, focus *is* Funput, so a toggle made there
//! leaves the tray icon stale until the panel closes.
//!
//! A thread timer covers that gap. It is not a check on every pumped message: the
//! low-level hooks wake this loop constantly, and stating the file that often is
//! wasted work. The timer is armed only while the flyout is the UI child, so an
//! idle tray is not woken. The flyout watches the same file for the other
//! direction (a hotkey), on its own timer.

use std::cell::Cell;
use std::time::SystemTime;

use windows::Win32::UI::WindowsAndMessaging::{KillTimer, SetTimer};

use crate::background::tray;
use crate::shared::{settings_path, shell};

/// How often to look. A toggle should show up on the icon before the click feels
/// ignored, without a file read on every mouse move the hook delivers.
const INTERVAL_MS: u32 = 250;

#[derive(Clone, Copy, PartialEq, Eq)]
struct Stamp {
    modified: SystemTime,
    len: u64,
}

#[derive(Clone, Copy)]
struct Watch {
    id: usize,
    seen: Option<Stamp>,
    open: bool,
}

impl Watch {
    const EMPTY: Self = Self {
        id: 0,
        seen: None,
        open: false,
    };
}

thread_local! {
    static WATCH: Cell<Watch> = const { Cell::new(Watch::EMPTY) };
}

/// Arm or drop the timer as the Control Center opens and closes.
pub(super) fn set_open(open: bool) {
    if WATCH.with(|cell| cell.get().open) == open {
        return;
    }
    edit(|watch| {
        watch.open = open;
        if open {
            arm(watch);
        } else {
            disarm(watch);
        }
    });
}

/// `true` when `id` is this thread's timer, which has then been handled.
///
/// A thread timer (`SetTimer` with a null window) arrives as `WM_TIMER` on the
/// hook pump. Dispatching it would drop it: there is no window procedure.
pub(super) fn deliver(id: usize) -> bool {
    let ours = WATCH.with(|cell| {
        let watch = cell.get();
        id != 0 && id == watch.id
    });
    if ours {
        on_timer();
    }
    ours
}

fn arm(watch: &mut Watch) {
    // The open itself is not a change. The next tick reloads only if the child
    // has written since.
    watch.seen = file_stamp();
    // SAFETY: a null window associates the timer with this thread. The returned
    // id is what later `WM_TIMER` messages carry, and what `KillTimer` takes.
    // Zero means Windows refused; the icon then updates when the flyout closes,
    // which is the path this timer exists to improve on.
    let id = unsafe { SetTimer(None, 0, INTERVAL_MS, None) };
    if id == 0 {
        eprintln!("Funput: failed to arm the Control Center settings timer");
    }
    watch.id = id;
}

fn disarm(watch: &mut Watch) {
    let id = watch.id;
    watch.id = 0;
    watch.seen = None;
    if id != 0 {
        let _ = unsafe { KillTimer(None, id) };
    }
}

fn on_timer() {
    let Some(now) = file_stamp() else {
        return;
    };
    if WATCH.with(|cell| cell.get().seen) == Some(now) {
        return;
    }
    let changed = shell::reload_settings();
    // Carrying a VI/EN flip onto the last app's pin writes the file again.
    // Record the stamp after that write, or the next tick treats it as news.
    edit(|watch| watch.seen = file_stamp());
    if changed {
        tray::sync_from_shell();
    }
}

fn file_stamp() -> Option<Stamp> {
    let path = settings_path::settings_path()?;
    let meta = std::fs::metadata(path).ok()?;
    Some(Stamp {
        modified: meta.modified().ok()?,
        len: meta.len(),
    })
}

fn edit(f: impl FnOnce(&mut Watch)) {
    WATCH.with(|cell| {
        let mut watch = cell.get();
        f(&mut watch);
        cell.set(watch);
    });
}
