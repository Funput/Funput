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
    phase: Phase,
}

const ESC: u8 = 0x1b;

impl Classifier {
    pub fn new(toggle: u8) -> Self {
        Self {
            toggle,
            phase: Phase::Normal,
        }
    }

    pub fn classify(&mut self, byte: u8) -> ByteKind {
        match self.phase {
            Phase::Normal => self.classify_normal(byte),
            Phase::AfterEsc => {
                // `ESC [` (CSI) or `ESC O` (SS3) start a multi-byte sequence;
                // anything else is a 2-byte sequence (e.g. Alt+key).
                self.phase = if byte == b'[' || byte == b'O' {
                    Phase::Csi
                } else {
                    Phase::Normal
                };
                ByteKind::Escape
            }
            Phase::Csi => {
                // Final byte of a CSI/SS3 sequence is in `0x40..=0x7e`.
                if (0x40..=0x7e).contains(&byte) {
                    self.phase = Phase::Normal;
                }
                ByteKind::Escape
            }
        }
    }

    fn classify_normal(&mut self, byte: u8) -> ByteKind {
        if byte == self.toggle {
            return ByteKind::Toggle;
        }
        match byte {
            ESC => {
                self.phase = Phase::AfterEsc;
                ByteKind::Escape
            }
            0x20..=0x7e => ByteKind::Printable(byte as char),
            0x80..=0xff => ByteKind::Utf8,
            _ => ByteKind::Control,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const CTRL_BACKSLASH: u8 = 0x1c;

    fn classify_all(toggle: u8, bytes: &[u8]) -> Vec<ByteKind> {
        let mut c = Classifier::new(toggle);
        bytes.iter().map(|&b| c.classify(b)).collect()
    }

    #[test]
    fn printable_letters_and_space() {
        assert_eq!(
            classify_all(CTRL_BACKSLASH, b"a s"),
            vec![
                ByteKind::Printable('a'),
                ByteKind::Printable(' '),
                ByteKind::Printable('s'),
            ]
        );
    }

    #[test]
    fn control_bytes() {
        // Enter, Tab, Backspace, Ctrl-C.
        assert_eq!(
            classify_all(CTRL_BACKSLASH, &[0x0d, 0x09, 0x7f, 0x03]),
            vec![ByteKind::Control; 4]
        );
    }

    #[test]
    fn toggle_key_recognised() {
        assert_eq!(
            classify_all(CTRL_BACKSLASH, &[CTRL_BACKSLASH]),
            vec![ByteKind::Toggle]
        );
    }

    #[test]
    fn arrow_key_is_escape_sequence() {
        // Up arrow = ESC [ A — all three bytes are Escape, then back to normal.
        let mut c = Classifier::new(CTRL_BACKSLASH);
        assert_eq!(c.classify(0x1b), ByteKind::Escape);
        assert_eq!(c.classify(b'['), ByteKind::Escape);
        assert_eq!(c.classify(b'A'), ByteKind::Escape);
        assert_eq!(c.classify(b'a'), ByteKind::Printable('a')); // sequence ended
    }

    #[test]
    fn alt_key_is_two_byte_escape() {
        let mut c = Classifier::new(CTRL_BACKSLASH);
        assert_eq!(c.classify(0x1b), ByteKind::Escape);
        assert_eq!(c.classify(b'x'), ByteKind::Escape); // ESC x = Alt-x
        assert_eq!(c.classify(b'y'), ByteKind::Printable('y'));
    }

    #[test]
    fn utf8_bytes_passthrough() {
        // "á" = 0xC3 0xA1
        assert_eq!(
            classify_all(CTRL_BACKSLASH, "á".as_bytes()),
            vec![ByteKind::Utf8, ByteKind::Utf8]
        );
    }
}
