//! Child → real-terminal forwarding, with alt-screen detection.

use std::io::{self, Read, Write};

use crate::state::SharedState;

// Alt-screen enter/leave sequences (xterm). Entering = full-screen TUI (vim, less…).
const ENTER_1049: &[u8] = b"\x1b[?1049h";
const LEAVE_1049: &[u8] = b"\x1b[?1049l";
const ENTER_47: &[u8] = b"\x1b[?47h";
const LEAVE_47: &[u8] = b"\x1b[?47l";
const MAX_SEQ: usize = ENTER_1049.len();

/// Scans the child's output stream for alt-screen transitions, tolerating
/// sequences split across read chunks via a small carry buffer.
struct AltScreenScanner {
    carry: Vec<u8>,
}

impl AltScreenScanner {
    fn new() -> Self {
        Self { carry: Vec::new() }
    }

    fn scan(&mut self, chunk: &[u8], state: &SharedState) {
        let mut buf = std::mem::take(&mut self.carry);
        buf.extend_from_slice(chunk);

        for i in 0..buf.len() {
            let rest = &buf[i..];
            if rest.starts_with(ENTER_1049) || rest.starts_with(ENTER_47) {
                state.set_alt_screen(true);
            } else if rest.starts_with(LEAVE_1049) || rest.starts_with(LEAVE_47) {
                state.set_alt_screen(false);
            }
        }

        // Keep the tail that might be the start of a sequence split across chunks.
        let keep = MAX_SEQ - 1;
        let start = buf.len().saturating_sub(keep);
        self.carry = buf[start..].to_vec();
    }
}

/// Forward child output to `writer` verbatim, updating alt-screen state as we go.
pub fn forward_output<R: Read, W: Write>(
    mut reader: R,
    mut writer: W,
    state: &SharedState,
) -> io::Result<()> {
    let mut scanner = AltScreenScanner::new();
    let mut buf = [0u8; 8192];
    loop {
        let n = reader.read(&mut buf)?;
        if n == 0 {
            break;
        }
        scanner.scan(&buf[..n], state);
        writer.write_all(&buf[..n])?;
        writer.flush()?;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_alt_screen_enter_and_leave() {
        let state = SharedState::new(true);
        let mut scanner = AltScreenScanner::new();

        scanner.scan(b"hello \x1b[?1049h world", &state);
        assert!(!state.composing(), "alt-screen should pause composing");

        scanner.scan(b"bye \x1b[?1049l", &state);
        assert!(state.composing(), "leaving alt-screen resumes composing");
    }

    #[test]
    fn handles_sequence_split_across_chunks() {
        let state = SharedState::new(true);
        let mut scanner = AltScreenScanner::new();
        scanner.scan(b"abc\x1b[?10", &state);
        scanner.scan(b"49h", &state);
        assert!(!state.composing());
    }
}
