//! Windows console setup: crossterm's raw mode only disables line/echo input, so on
//! its own the console does not deliver VT input (arrow/escape keys never reach
//! `stdin` as bytes) nor render the VT the ConPTY child emits. This puts the console
//! into the same VT byte-stream shape a Unix PTY has, and switches both code pages to
//! UTF-8 so multibyte Vietnamese round-trips. All changes are undone on drop.

use windows::Win32::Foundation::HANDLE;
use windows::Win32::System::Console::{
    CONSOLE_MODE, DISABLE_NEWLINE_AUTO_RETURN, ENABLE_VIRTUAL_TERMINAL_INPUT,
    ENABLE_VIRTUAL_TERMINAL_PROCESSING, GetConsoleCP, GetConsoleMode, GetConsoleOutputCP,
    GetStdHandle, STD_HANDLE, STD_INPUT_HANDLE, STD_OUTPUT_HANDLE, SetConsoleCP, SetConsoleMode,
    SetConsoleOutputCP,
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
