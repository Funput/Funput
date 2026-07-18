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
    if method.is_telex_family() {
        InputMethod::Vni
    } else {
        InputMethod::Telex
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
mod tests;
