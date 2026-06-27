//! In-process Vietnamese composition for the Settings window's gõ tắt expansion
//! field. The global keyboard hook can't compose into Funput's own Slint window
//! (winit ignores synthetic input), so the field feeds each real keystroke through
//! this composer — driving the same `funput-engine` — and shows the result directly.
//! Lives on the UI (main) thread; separate from the hook's engine.

use funput_core::{InputMethod, ToneStyle};
use funput_engine::Engine;

/// Builds the field text as `committed` (finished words) + the engine's live buffer
/// (the word being composed). Only the trailing word is "hot"; a word boundary or a
/// reset folds the buffer into `committed`.
pub struct FieldComposer {
    engine: Engine,
    committed: String,
}

impl FieldComposer {
    pub fn new() -> Self {
        let mut engine = Engine::new();
        // A config field keeps what the user composes (no English auto-restore).
        engine.set_smart_restore(false);
        Self {
            engine,
            committed: String::new(),
        }
    }

    fn current(&self) -> String {
        format!("{}{}", self.committed, self.engine.buffer())
    }

    /// Start a fresh composition with `text` already in the field (focus-in), applying
    /// the user's current method/tone so it matches global typing.
    pub fn reset(&mut self, text: &str, method: InputMethod, tone: ToneStyle) {
        self.engine.set_method(method);
        self.engine.set_tone_style(tone);
        self.engine.clear();
        self.committed = text.to_string();
    }

    /// Feed one typed character; returns the new full field text.
    pub fn key(&mut self, c: char) -> String {
        if c.is_whitespace() || c.is_ascii_punctuation() {
            // Word boundary: fold the composed word + this separator into committed.
            self.committed.push_str(self.engine.buffer());
            self.committed.push(c);
            self.engine.clear();
        } else {
            self.engine.process_char(c);
        }
        self.current()
    }

    /// Backspace; returns the new full field text.
    pub fn backspace(&mut self) -> String {
        if self.engine.buffer().is_empty() {
            self.committed.pop();
        } else {
            self.engine.on_backspace();
        }
        self.current()
    }
}

impl Default for FieldComposer {
    fn default() -> Self {
        Self::new()
    }
}
