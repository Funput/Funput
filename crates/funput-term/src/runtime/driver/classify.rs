//! Pure classification of the raw keyboard byte stream.
//!
//! The driver feeds bytes one at a time; the classifier tracks just enough state
//! to recognise escape sequences (arrows, function keys, Alt-combos) so they are
//! never mistaken for composable letters.

/// What a single input byte means.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ByteKind {
    /// Printable ASCII (`0x20..=0x7e`) — a candidate to feed the engine.
    Printable(char),
    /// Control byte (Enter, Tab, Backspace, Ctrl-key) — forward raw, flush composition.
    Control,
    /// Part of an escape sequence — forward raw, flush composition.
    Escape,
    /// UTF-8 lead/continuation (`>= 0x80`) — forward raw (pasted/precomposed text).
    Utf8,
    /// The configured toggle key — consume, do not forward.
    Toggle,
    /// The configured cycle-method key (Telex↔VNI) — consume, do not forward.
    CycleMethod,
    /// Byte inside a bracketed paste — forward raw, never compose.
    Paste,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Phase {
    Normal,
    /// Saw `ESC`; the next byte decides the sequence kind.
    AfterEsc,
    /// Inside a CSI (`ESC[`) or SS3 (`ESC O`) sequence.
    Csi,
}

/// Byte-stream classifier with minimal escape-sequence state.
#[derive(Debug)]
pub struct Classifier {
    toggle: u8,
    /// Key that cycles Telex↔VNI, or `None` when disabled.
    cycle_method: Option<u8>,
    phase: Phase,
    /// Inside a bracketed paste (`ESC[200~` … `ESC[201~`): forward content raw.
    in_paste: bool,
    /// Parameter bytes of the CSI sequence being parsed, kept only to recognise
    /// the `200`/`201` bracketed-paste markers. Capped at `MAX_CSI_PARAMS`.
    params: Vec<u8>,
}

const ESC: u8 = 0x1b;
/// Bracketed-paste markers (`ESC[200~` start, `ESC[201~` end).
const PASTE_START: &[u8] = b"200";
const PASTE_END: &[u8] = b"201";
/// Upper bound on retained CSI parameter bytes — enough for the markers we match,
/// while keeping a malformed, never-terminated CSI from growing the buffer.
const MAX_CSI_PARAMS: usize = PASTE_START.len();

impl Classifier {
    pub fn new(toggle: u8, cycle_method: Option<u8>) -> Self {
        Self {
            toggle,
            cycle_method,
            phase: Phase::Normal,
            in_paste: false,
            params: Vec::new(),
        }
    }

    pub fn classify(&mut self, byte: u8) -> ByteKind {
        match self.phase {
            Phase::Normal => self.classify_normal(byte),
            Phase::AfterEsc => {
                // `ESC [` (CSI) or `ESC O` (SS3) start a multi-byte sequence;
                // anything else is a 2-byte sequence (e.g. Alt+key).
                self.phase = if byte == b'[' || byte == b'O' {
                    self.params.clear();
                    Phase::Csi
                } else {
                    Phase::Normal
                };
                ByteKind::Escape
            }
            Phase::Csi => {
                // Final byte of a CSI/SS3 sequence is in `0x40..=0x7e`; bytes
                // before it are parameters we track to spot the bracketed-paste
                // markers.
                if (0x40..=0x7e).contains(&byte) {
                    self.phase = Phase::Normal;
                    if byte == b'~' {
                        match self.params.as_slice() {
                            PASTE_START => self.in_paste = true,
                            PASTE_END => self.in_paste = false,
                            _ => {}
                        }
                    }
                } else if self.params.len() <= MAX_CSI_PARAMS {
                    // Retain one byte beyond a marker's length so an over-long
                    // run (e.g. `2000`) can't equal a marker, then stop growing.
                    self.params.push(byte);
                }
                ByteKind::Escape
            }
        }
    }

    fn classify_normal(&mut self, byte: u8) -> ByteKind {
        if byte == ESC {
            self.phase = Phase::AfterEsc;
            return ByteKind::Escape;
        }
        // Inside a paste, every non-ESC byte is literal content: forward it raw
        // before any toggle/printable handling, so pasted letters and even the
        // toggle key are never interpreted as commands.
        if self.in_paste {
            return ByteKind::Paste;
        }
        if byte == self.toggle {
            return ByteKind::Toggle;
        }
        if self.cycle_method == Some(byte) {
            return ByteKind::CycleMethod;
        }
        match byte {
            0x20..=0x7e => ByteKind::Printable(byte as char),
            0x80..=0xff => ByteKind::Utf8,
            _ => ByteKind::Control,
        }
    }
}

#[cfg(test)]
mod tests;
