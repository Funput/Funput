use super::*;

const CTRL_BACKSLASH: u8 = 0x1c;

fn classify_all(toggle: u8, bytes: &[u8]) -> Vec<ByteKind> {
    let mut c = Classifier::new(toggle, None);
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
fn cycle_method_key_recognised_only_when_configured() {
    const CTRL_CARET: u8 = 0x1e;
    // Configured → CycleMethod; nothing else changes.
    let mut c = Classifier::new(CTRL_BACKSLASH, Some(CTRL_CARET));
    assert_eq!(c.classify(CTRL_CARET), ByteKind::CycleMethod);
    assert_eq!(c.classify(CTRL_BACKSLASH), ByteKind::Toggle);
    // Disabled → the same byte is just a control byte.
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
    assert_eq!(c.classify(CTRL_CARET), ByteKind::Control);
}

#[test]
fn arrow_key_is_escape_sequence() {
    // Up arrow = ESC [ A — all three bytes are Escape, then back to normal.
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
    assert_eq!(c.classify(0x1b), ByteKind::Escape);
    assert_eq!(c.classify(b'['), ByteKind::Escape);
    assert_eq!(c.classify(b'A'), ByteKind::Escape);
    assert_eq!(c.classify(b'a'), ByteKind::Printable('a')); // sequence ended
}

#[test]
fn alt_key_is_two_byte_escape() {
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
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

#[test]
fn bracketed_paste_content_is_raw() {
    // ESC[200~as ESC[201~b : "as" is paste content (not composed), then
    // "b" composes normally once the paste ends.
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
    for &b in b"\x1b[200~" {
        assert_eq!(c.classify(b), ByteKind::Escape);
    }
    assert_eq!(c.classify(b'a'), ByteKind::Paste);
    assert_eq!(c.classify(b's'), ByteKind::Paste);
    for &b in b"\x1b[201~" {
        assert_eq!(c.classify(b), ByteKind::Escape);
    }
    assert_eq!(c.classify(b'b'), ByteKind::Printable('b'));
}

#[test]
fn paste_marker_split_across_chunks() {
    // The marker can arrive byte-by-byte across reads; classifier state
    // persists, so paste mode still engages.
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
    for &b in b"\x1b[20" {
        c.classify(b);
    }
    for &b in b"0~" {
        c.classify(b);
    }
    assert_eq!(c.classify(b'x'), ByteKind::Paste);
}

#[test]
fn toggle_and_letters_inside_paste_are_raw() {
    // Pasted content must never be interpreted as commands: the toggle key
    // and letters alike are literal Paste bytes.
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
    for &b in b"\x1b[200~" {
        c.classify(b);
    }
    assert_eq!(c.classify(CTRL_BACKSLASH), ByteKind::Paste);
    assert_eq!(c.classify(b'a'), ByteKind::Paste);
}

#[test]
fn over_long_csi_is_not_a_paste_marker() {
    // A CSI whose parameters exceed a marker's length must not toggle paste,
    // and its parameter buffer must stay bounded.
    let mut c = Classifier::new(CTRL_BACKSLASH, None);
    for &b in b"\x1b[200000~" {
        c.classify(b);
    }
    assert_eq!(c.classify(b'a'), ByteKind::Printable('a'));
    assert!(c.params.len() <= MAX_CSI_PARAMS + 1);
}
