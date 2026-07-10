//! Real-terminal helpers: the raw-mode guard, plus the composition-state
//! indicators ([`indicator`]) and the Windows console shim ([`console`]).

use std::io;

use crossterm::terminal::{disable_raw_mode, enable_raw_mode};

#[cfg(windows)]
mod console;
mod indicator;

pub(crate) use indicator::{DEFAULT_VI_CURSOR_COLOR, set_cursor_cue, set_title};

/// Enables raw mode and restores it on drop — including on panic or early return,
/// so the user's terminal is never left in a broken state.
///
/// On Windows it additionally switches the console into a VT byte-stream mode
/// (see [`console`]) so the interposer sees the same kind of stream a Unix PTY gives.
pub struct RawModeGuard {
    #[cfg(windows)]
    restore: Option<console::ConsoleRestore>,
}

impl RawModeGuard {
    pub fn enter() -> io::Result<Self> {
        enable_raw_mode()?;
        Ok(Self {
            #[cfg(windows)]
            restore: Some(console::setup()),
        })
    }
}

impl Drop for RawModeGuard {
    fn drop(&mut self) {
        // Restore console VT/codepage state before crossterm restores the raw mode.
        #[cfg(windows)]
        if let Some(restore) = self.restore.take() {
            restore.restore();
        }
        let _ = disable_raw_mode();
    }
}
