//! Real-terminal helpers: raw-mode guard and window-title indicator.

use std::io::{self, Write};

use crossterm::terminal::{disable_raw_mode, enable_raw_mode};

/// Enables raw mode and restores it on drop — including on panic or early return,
/// so the user's terminal is never left in a broken state.
pub struct RawModeGuard;

impl RawModeGuard {
    pub fn enter() -> io::Result<Self> {
        enable_raw_mode()?;
        Ok(Self)
    }
}

impl Drop for RawModeGuard {
    fn drop(&mut self) {
        let _ = disable_raw_mode();
    }
}

/// Set the terminal window title via OSC — a non-intrusive status indicator that
/// does not draw over the child app's UI.
pub fn set_title(out: &mut impl Write, text: &str) -> io::Result<()> {
    write!(out, "\x1b]0;{text}\x07")?;
    out.flush()
}
