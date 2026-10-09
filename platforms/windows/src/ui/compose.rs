//! In-process Vietnamese composition for the Settings window's gõ tắt expansion
//! field. The global keyboard hook can't compose into Funput's own Slint window
//! (winit ignores synthetic input), so the field feeds each real keystroke through
//! this composer — driving the same `funput-engine` — and shows the result directly.
//! Lives on the UI (main) thread; separate from the hook's engine.

use funput_config::Settings;
use funput_core::InputMethod;
use funput_engine::Engine;

/// Builds the field text as `committed` (finished words) + the engine's live buffer
/// (the word being composed). Only the trailing word is "hot"; a word boundary or a
/// reset folds the buffer into `committed`.
pub struct FieldComposer {
    engine: Engine,
    method: InputMethod,
    committed: String,
}

impl FieldComposer {
    pub fn new() -> Self {
        let mut engine = Engine::new();
        // A config field keeps what the user composes (no English auto-restore).
        engine.update_config(|c| c.smart_restore = false);
        Self {
            engine,
            method: InputMethod::Telex,
            committed: String::new(),
        }
    }

    fn current(&self) -> String {
        format!("{}{}", self.committed, self.engine.buffer())
    }

    /// Start a fresh composition with `text` already in the field (focus-in), applying
    /// the user's spelling choices — method, tone placement, extra onsets — so the
    /// field composes the way global typing does.
    pub fn reset(&mut self, text: &str, settings: &Settings) {
        self.method = settings.method.core();
        self.engine.set_method(self.method);
        self.engine.update_config(|c| {
            c.tone_style = settings.tone_style.core();
            c.syllable_rules = c
                .syllable_rules
                .with_extra_onsets(settings.extra_onsets.core());
        });
        self.engine.clear();
        self.committed = text.to_string();
    }

    /// Feed one typed character; returns the new full field text.
    pub fn key(&mut self, c: char) -> String {
        // Slint reports modifier keys (Shift/Ctrl/…), F-keys and other non-text keys
        // as control or Private-Use-Area characters. Ignore them — only real text
        // composes. (Backspace/navigation are handled before reaching here.)
        if !is_text(c) {
            return self.current();
        }
        if c.is_whitespace() || (c.is_ascii_punctuation() && !self.is_advanced_shortcut(c)) {
            // Word boundary: fold the composed word + this separator into committed.
            self.committed.push_str(self.engine.buffer());
            self.committed.push(c);
            self.engine.clear();
        } else {
            self.engine.process_char(c);
            if self.engine.buffer().is_empty() {
                // The engine passed the key through (e.g. a digit starting a word — a
                // number, not Vietnamese). It won't show up in the buffer, so fold it
                // into the committed text directly.
                self.committed.push(c);
            }
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

    fn is_advanced_shortcut(&self, c: char) -> bool {
        self.method == InputMethod::TelexAdvanced && matches!(c, '[' | ']')
    }
}

impl Default for FieldComposer {
    fn default() -> Self {
        Self::new()
    }
}

/// Whether `c` is real typed text (a letter, digit, space, punctuation, accented or
/// CJK character) rather than a control char or a Slint special-key codepoint
/// (modifier keys, arrows, F-keys live in the Unicode Private Use Areas).
fn is_text(c: char) -> bool {
    if c.is_control() {
        return false;
    }
    let u = c as u32;
    let private_use = (0xE000..=0xF8FF).contains(&u)
        || (0xF_0000..=0xF_FFFD).contains(&u)
        || (0x10_0000..=0x10_FFFD).contains(&u);
    !private_use
}

#[cfg(test)]
mod tests {
    use funput_config::{ExtraOnsetLetters, Method, Settings};

    use super::FieldComposer;

    fn typed(keys: &str, letters: &str) -> String {
        let settings = Settings {
            method: Method::Telex,
            extra_onsets: ExtraOnsetLetters::from_id(letters),
            ..Settings::default()
        };
        let mut composer = FieldComposer::new();
        composer.reset("", &settings);
        keys.chars().fold(String::new(), |_, c| composer.key(c))
    }

    /// The gõ tắt field composes the way global typing does, extra onsets included.
    #[test]
    fn the_field_follows_the_chosen_onsets() {
        assert_eq!(typed("zoo jowf", "z"), "zô jowf");
        assert_eq!(typed("zoo jowf", "zj"), "zô jờ");
    }
}
