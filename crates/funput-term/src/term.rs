//! Real-terminal helpers: raw-mode guard and window-title indicator.

use std::io::{self, Write};

use crossterm::terminal::{disable_raw_mode, enable_raw_mode};

/// Enables raw mode and restores it on drop — including on panic or early return,
/// so the user's terminal is never left in a broken state.
///
/// On Windows it additionally switches the console into a VT byte-stream mode
/// (see [`win`]) so the interposer sees the same kind of stream a Unix PTY gives.
pub struct RawModeGuard {
    #[cfg(windows)]
    restore: Option<win::ConsoleRestore>,
}

impl RawModeGuard {
    pub fn enter() -> io::Result<Self> {
        enable_raw_mode()?;
        Ok(Self {
            #[cfg(windows)]
            restore: Some(win::setup()),
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

/// Windows console setup: crossterm's raw mode only disables line/echo input, so on
/// its own the console does not deliver VT input (arrow/escape keys never reach
/// `stdin` as bytes) nor render the VT the ConPTY child emits. This puts the console
/// into the same VT byte-stream shape a Unix PTY has, and switches both code pages to
/// UTF-8 so multibyte Vietnamese round-trips. All changes are undone on drop.
#[cfg(windows)]
mod win {
    use windows::Win32::Foundation::HANDLE;
    use windows::Win32::System::Console::{
        CONSOLE_MODE, DISABLE_NEWLINE_AUTO_RETURN, ENABLE_VIRTUAL_TERMINAL_INPUT,
        ENABLE_VIRTUAL_TERMINAL_PROCESSING, GetConsoleCP, GetConsoleMode, GetConsoleOutputCP,
        GetStdHandle, STD_HANDLE, STD_INPUT_HANDLE, STD_OUTPUT_HANDLE, SetConsoleCP,
        SetConsoleMode, SetConsoleOutputCP,
    };

    /// UTF-8 code page id, used for both console input and output so multibyte
    /// Vietnamese (and pasted Unicode) flows through the byte stream intact.
    const CP_UTF8: u32 = 65001;

    /// Original console state captured by [`setup`], reapplied on drop of the guard.
    pub struct ConsoleRestore {
        stdin: Option<(HANDLE, CONSOLE_MODE)>,
        stdout: Option<(HANDLE, CONSOLE_MODE)>,
        in_cp: u32,
        out_cp: u32,
    }

    /// Enable VT input on stdin and VT processing on stdout, switch both code pages
    /// to UTF-8, and return a restorer for the previous state. Best effort: a handle
    /// with no console (e.g. redirected to a pipe) is left as-is with nothing to undo.
    pub fn setup() -> ConsoleRestore {
        // SAFETY: plain FFI reads of the current console code pages.
        let in_cp = unsafe { GetConsoleCP() };
        let out_cp = unsafe { GetConsoleOutputCP() };
        unsafe {
            let _ = SetConsoleCP(CP_UTF8);
            let _ = SetConsoleOutputCP(CP_UTF8);
        }

        let stdin = add_flags(STD_INPUT_HANDLE, ENABLE_VIRTUAL_TERMINAL_INPUT);
        let stdout = add_flags(
            STD_OUTPUT_HANDLE,
            ENABLE_VIRTUAL_TERMINAL_PROCESSING | DISABLE_NEWLINE_AUTO_RETURN,
        );

        ConsoleRestore {
            stdin,
            stdout,
            in_cp,
            out_cp,
        }
    }

    /// OR `flags` into the console mode of a standard handle, returning the handle and
    /// its original mode for later restore. `None` if the handle has no console.
    fn add_flags(which: STD_HANDLE, flags: CONSOLE_MODE) -> Option<(HANDLE, CONSOLE_MODE)> {
        // SAFETY: standard Win32 console mode get/set on a handle we own.
        unsafe {
            let handle = GetStdHandle(which).ok()?;
            let mut mode = CONSOLE_MODE::default();
            GetConsoleMode(handle, &mut mode).ok()?;
            SetConsoleMode(handle, mode | flags).ok()?;
            Some((handle, mode))
        }
    }

    impl ConsoleRestore {
        /// Reapply the captured console modes and code pages.
        pub fn restore(self) {
            // SAFETY: restoring values we previously read from the same handles.
            unsafe {
                if let Some((handle, mode)) = self.stdin {
                    let _ = SetConsoleMode(handle, mode);
                }
                if let Some((handle, mode)) = self.stdout {
                    let _ = SetConsoleMode(handle, mode);
                }
                let _ = SetConsoleCP(self.in_cp);
                let _ = SetConsoleOutputCP(self.out_cp);
            }
        }
    }
}

/// Terminal multiplexer wrapping the session, if any. A bare OSC title sequence
/// is swallowed by tmux/screen, so it must be wrapped in their passthrough form
/// to reach the outer terminal.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Mux {
    None,
    Tmux,
    Screen,
}

/// Detect the surrounding multiplexer from the environment. `$TMUX` is set inside
/// tmux; GNU screen is recognised by `$STY` or a `screen`/`tmux` `$TERM` (best
/// effort, since screen's passthrough is more limited).
pub fn detect_mux() -> Mux {
    if std::env::var_os("TMUX").is_some() {
        return Mux::Tmux;
    }
    let term = std::env::var("TERM").unwrap_or_default();
    if std::env::var_os("STY").is_some() || term.starts_with("screen") || term.starts_with("tmux") {
        return Mux::Screen;
    }
    Mux::None
}

/// Default cursor color shown while Vietnamese composition is on — the brand
/// green used in the README badges.
pub const DEFAULT_VI_CURSOR_COLOR: &str = "#22C55E";

/// Wrap a terminal control sequence so it survives the surrounding multiplexer
/// and reaches the outer terminal.
///
/// Pure (no I/O) so the exact wrapping is unit-tested.
fn wrap_for_mux(seq: &str, mux: Mux) -> String {
    match mux {
        Mux::None => seq.to_string(),
        // tmux DCS passthrough: every inner ESC must be doubled.
        Mux::Tmux => format!("\x1bPtmux;{}\x1b\\", seq.replace('\x1b', "\x1b\x1b")),
        // screen DCS passthrough: forward the sequence as-is.
        Mux::Screen => format!("\x1bP{seq}\x1b\\"),
    }
}

/// Build the byte sequence that sets the terminal window title to `text`,
/// wrapped for `mux` so it reaches the outer terminal.
pub fn title_sequence(text: &str, mux: Mux) -> String {
    wrap_for_mux(&format!("\x1b]0;{text}\x07"), mux)
}

/// Build the cursor-color cue: a brand-colored cursor while composing (VI), reset
/// to the terminal default otherwise (EN). A non-intrusive indicator that works
/// even when the window title is hidden.
///
/// VI sets the cursor color via `OSC 12`; EN resets it via `OSC 112` rather than
/// guessing the user's default. Wrapped for `mux` like the title.
pub fn cursor_cue(enabled: bool, vi_color: &str, mux: Mux) -> String {
    let seq = if enabled {
        format!("\x1b]12;{vi_color}\x07")
    } else {
        "\x1b]112\x07".to_string()
    };
    wrap_for_mux(&seq, mux)
}

/// Set the terminal window title — a non-intrusive status indicator that does not
/// draw over the child app's UI, wrapped for any surrounding multiplexer.
pub fn set_title(out: &mut impl Write, text: &str) -> io::Result<()> {
    out.write_all(title_sequence(text, detect_mux()).as_bytes())?;
    out.flush()
}

/// Update the cursor-color cue to match the composition state. Always safe to
/// call with `enabled = false` on exit to restore the user's default cursor.
pub fn set_cursor_cue(out: &mut impl Write, enabled: bool, vi_color: &str) -> io::Result<()> {
    out.write_all(cursor_cue(enabled, vi_color, detect_mux()).as_bytes())?;
    out.flush()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn plain_title_is_bare_osc() {
        assert_eq!(
            title_sequence("Funput · VI", Mux::None),
            "\x1b]0;Funput · VI\x07"
        );
    }

    #[test]
    fn tmux_title_doubles_inner_escapes() {
        assert_eq!(
            title_sequence("VI", Mux::Tmux),
            "\x1bPtmux;\x1b\x1b]0;VI\x07\x1b\\"
        );
    }

    #[test]
    fn screen_title_wraps_in_dcs() {
        assert_eq!(
            title_sequence("VI", Mux::Screen),
            "\x1bP\x1b]0;VI\x07\x1b\\"
        );
    }

    #[test]
    fn cursor_cue_sets_color_when_enabled() {
        assert_eq!(
            cursor_cue(true, "#22C55E", Mux::None),
            "\x1b]12;#22C55E\x07"
        );
    }

    #[test]
    fn cursor_cue_resets_when_disabled() {
        assert_eq!(cursor_cue(false, "#22C55E", Mux::None), "\x1b]112\x07");
    }

    #[test]
    fn cursor_cue_wraps_for_tmux() {
        // Inner ESC doubled inside the tmux DCS passthrough.
        assert_eq!(
            cursor_cue(true, "#fff", Mux::Tmux),
            "\x1bPtmux;\x1b\x1b]12;#fff\x07\x1b\\"
        );
    }

    #[test]
    fn cursor_cue_wraps_for_screen() {
        assert_eq!(
            cursor_cue(false, "#fff", Mux::Screen),
            "\x1bP\x1b]112\x07\x1b\\"
        );
    }
}
