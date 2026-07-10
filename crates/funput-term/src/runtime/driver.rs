//! The pure input seam: read keystrokes, compose Vietnamese, write result bytes.
//!
//! Pure of real I/O — the caller injects the reader/writer and a status callback,
//! so this is unit-tested with in-memory pipes.

use std::io::{self, Read, Write};

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use super::inject::result_bytes;
use super::input::{ByteKind, Classifier};
use super::state::SharedState;
use crate::config::TermConfig;

/// The user-visible composition state, reported whenever it changes so the caller
/// can refresh the indicators (window title + cursor cue).
#[derive(Debug, Clone, Copy)]
pub struct Status {
    /// VI (composing) vs EN (passthrough).
    pub enabled: bool,
    /// Active input method.
    pub method: InputMethod,
}

/// The other input method — used to cycle Telex↔VNI.
fn other_method(method: InputMethod) -> InputMethod {
    match method {
        InputMethod::Telex => InputMethod::Vni,
        InputMethod::Vni => InputMethod::Telex,
    }
}

/// Read keystrokes from `reader`, compose, and write the result bytes to `writer`.
///
/// Pure of real I/O — the caller injects the reader/writer and a toggle callback,
/// so this is unit-tested with in-memory pipes.
pub fn forward_input<R, W, F>(
    mut reader: R,
    mut writer: W,
    config: &TermConfig,
    state: &SharedState,
    mut on_status: F,
) -> io::Result<()>
where
    R: Read,
    W: Write,
    F: FnMut(Status),
{
    let mut engine = Engine::new();
    config.apply_to(&mut engine);
    engine.arm_capitalization();
    let mut classifier = Classifier::new(config.toggle, config.cycle_method);
    let mut buf = [0u8; 4096];

    loop {
        let n = reader.read(&mut buf)?;
        if n == 0 {
            break;
        }
        for &byte in &buf[..n] {
            match classifier.classify(byte) {
                ByteKind::Toggle => {
                    let enabled = state.toggle();
                    engine.clear();
                    on_status(Status {
                        enabled,
                        method: engine.method(),
                    });
                }
                // Cycle Telex↔VNI live; flush the in-progress word so it doesn't
                // carry half-composed under the old method.
                ByteKind::CycleMethod => {
                    let method = other_method(engine.method());
                    engine.set_method(method);
                    engine.clear();
                    on_status(Status {
                        enabled: state.enabled(),
                        method,
                    });
                }
                ByteKind::Printable(ch) if state.composing() => {
                    let result = engine.process_char(ch);
                    writer.write_all(&result_bytes(ch, &result))?;
                }
                // Backspace: drop the last char from the composition so the next
                // key composes against the corrected text ("Phua" ⌫ "s" → "Phú"),
                // then pass it so the app deletes its own last char.
                ByteKind::Control if state.composing() && is_backspace(byte) => {
                    engine.on_backspace();
                    writer.write_all(&[byte])?;
                }
                // Whitespace controls (Enter, Tab) are word boundaries: route them
                // through the engine so English-restore fires before they reach the
                // child (e.g. typing "text"+Enter submits "text", not "tẽt").
                ByteKind::Control if state.composing() && is_ws_boundary(byte) => {
                    let ch = byte as char;
                    let result = engine.process_char(ch);
                    match result.action {
                        Action::None => writer.write_all(&[byte])?,
                        _ => writer.write_all(&result_bytes(ch, &result))?,
                    }
                }
                // Bracketed-paste content: forward verbatim, never compose. The
                // start marker already flushed composition via the arm below.
                ByteKind::Paste => writer.write_all(&[byte])?,
                // Control / escape / utf8 / printable-while-disabled: end the
                // current word and forward the byte unchanged.
                _ => {
                    engine.clear();
                    writer.write_all(&[byte])?;
                }
            }
        }
        writer.flush()?;
    }
    Ok(())
}

/// Whitespace control bytes that act as word boundaries (Tab, LF, CR/Enter).
fn is_ws_boundary(byte: u8) -> bool {
    matches!(byte, b'\t' | b'\n' | b'\r')
}

/// Backspace bytes — DEL (0x7f, common in terminals) or BS (0x08).
fn is_backspace(byte: u8) -> bool {
    matches!(byte, 0x7f | 0x08)
}

#[cfg(test)]
mod tests {
    use super::*;

    use funput_core::InputMethod;

    fn config_for(method: InputMethod) -> TermConfig {
        TermConfig {
            method,
            ..TermConfig::default()
        }
    }

    fn compose(method: InputMethod, input: &[u8]) -> Vec<u8> {
        let state = SharedState::new(true);
        let mut out = Vec::new();
        forward_input(input, &mut out, &config_for(method), &state, |_| {}).unwrap();
        out
    }

    #[test]
    fn telex_word_emits_backspace_and_unicode() {
        // "as": 'a' passes through, 's' deletes 'a' and injects 'á'.
        let mut expected = b"a".to_vec();
        expected.push(0x7f);
        expected.extend_from_slice("á".as_bytes());
        assert_eq!(compose(InputMethod::Telex, b"as"), expected);
    }

    /// Reconstruct the child's visible text from the byte stream we send it
    /// (DEL = backspace one char; other bytes are injected UTF-8).
    fn reconstruct(bytes: &[u8]) -> String {
        let mut text = String::new();
        let mut i = 0;
        while i < bytes.len() {
            let b = bytes[i];
            if b == 0x7f {
                text.pop();
                i += 1;
            } else {
                let len = match b {
                    _ if b < 0x80 => 1,
                    _ if b >> 5 == 0b110 => 2,
                    _ if b >> 4 == 0b1110 => 3,
                    _ => 4,
                };
                if let Ok(s) = std::str::from_utf8(&bytes[i..i + len]) {
                    text.push_str(s);
                }
                i += len;
            }
        }
        text
    }

    #[test]
    fn enter_triggers_english_restore() {
        // "text" composes to "tẽt", but Enter (a boundary) restores the raw word.
        let out = compose(InputMethod::Telex, b"text\r");
        assert_eq!(reconstruct(&out), "text\r");
    }

    #[test]
    fn backspace_corrects_mid_composition() {
        // "Phua" (typo), Backspace the "a", then "s" → "Phú".
        let out = compose(InputMethod::Telex, b"Phua\x7fs");
        assert_eq!(reconstruct(&out), "Phú");
    }

    #[test]
    fn double_modifier_revert_sends_no_extra_char() {
        // "mix" → "mĩ"; pressing 'x' again reverts to the raw "mix" (one x, not
        // "mixx"). Confirms funput-term emits the correct bytes.
        let out = compose(InputMethod::Telex, b"mixx");
        assert_eq!(reconstruct(&out), "mix");
    }

    #[test]
    fn enter_keeps_valid_vietnamese() {
        // A real syllable is finalized, not restored.
        let out = compose(InputMethod::Telex, b"mas\r");
        assert_eq!(reconstruct(&out), "má\r");
    }

    #[test]
    fn bracketed_paste_is_forwarded_verbatim() {
        // "as" pasted inside ESC[200~…ESC[201~ must stay "as", not compose to
        // "á"; the markers themselves are forwarded to the child untouched.
        let out = compose(InputMethod::Telex, b"\x1b[200~as\x1b[201~");
        assert!(out.starts_with(b"\x1b[200~"));
        assert!(out.ends_with(b"\x1b[201~"));
        assert_eq!(reconstruct(&out), "\x1b[200~as\x1b[201~");
    }

    #[test]
    fn composition_resumes_after_paste() {
        // Compose, paste raw, then compose again — all in one buffer.
        let out = compose(InputMethod::Telex, b"as\x1b[200~as\x1b[201~as");
        // First "as" → "á", pasted "as" stays literal, trailing "as" → "á".
        assert_eq!(reconstruct(&out), "á\x1b[200~as\x1b[201~á");
    }

    #[test]
    fn toggle_off_disables_composition() {
        let state = SharedState::new(true);
        let mut out = Vec::new();
        let mut toggles = Vec::new();
        // Ctrl-\ then "as": composition is off, so bytes pass through raw.
        forward_input(
            &[0x1c, b'a', b's'][..],
            &mut out,
            &config_for(InputMethod::Telex),
            &state,
            |status| toggles.push(status.enabled),
        )
        .unwrap();
        assert_eq!(out, b"as");
        assert_eq!(toggles, vec![false]);
        assert!(!state.composing());
    }

    #[test]
    fn cycle_method_key_switches_telex_vni() {
        // Start Telex (cycle key Ctrl-^ = 0x1e): "as" → "á". After cycling to VNI,
        // "as" stays "as" (s is not a VNI modifier). Status reports the new method.
        let config = TermConfig {
            cycle_method: Some(0x1e),
            ..config_for(InputMethod::Telex)
        };
        let state = SharedState::new(true);
        let mut out = Vec::new();
        let mut methods = Vec::new();
        forward_input(b"as\x1eas".as_ref(), &mut out, &config, &state, |s| {
            methods.push(s.method)
        })
        .unwrap();
        assert_eq!(reconstruct(&out), "áas");
        assert_eq!(methods, vec![InputMethod::Vni]);
    }

    #[test]
    fn config_disabled_composes_nothing() {
        // enabled = false in config → start in EN, keystrokes pass through raw.
        let config = TermConfig {
            enabled: false,
            ..config_for(InputMethod::Telex)
        };
        let state = SharedState::new(config.enabled);
        let mut out = Vec::new();
        forward_input(b"as".as_ref(), &mut out, &config, &state, |_| {}).unwrap();
        assert_eq!(out, b"as");
    }

    #[test]
    fn config_shortcut_expands_at_boundary() {
        // A gõ-tắt shortcut from config expands when the word boundary arrives.
        let config = TermConfig {
            shortcuts: vec![("vn".to_string(), "Việt Nam".to_string())],
            ..config_for(InputMethod::Telex)
        };
        let state = SharedState::new(true);
        let mut out = Vec::new();
        forward_input(b"vn ".as_ref(), &mut out, &config, &state, |_| {}).unwrap();
        assert_eq!(reconstruct(&out), "Việt Nam ");
    }
}
