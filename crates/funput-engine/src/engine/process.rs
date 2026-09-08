use crate::compose::{boundary, pipeline};
use crate::{Engine, ImeResult, KeySource};

impl Engine {
    /// Process one Unicode scalar from the main keyboard. Shorthand for
    /// [`Engine::process_key`] with [`KeySource::Standard`].
    pub fn process_char(&mut self, key: char) -> ImeResult {
        self.process_key(key, KeySource::Standard)
    }

    /// Process one Unicode scalar tagged with its physical [`KeySource`], returning
    /// the platform edit instruction.
    ///
    /// A numpad digit is emitted as a literal number: it ends the current word like
    /// a boundary (committing or restoring the buffer) instead of acting as a VNI
    /// tone/shape modifier. Every other key behaves exactly as [`Engine::process_char`].
    ///
    /// A disabled engine is **not** a no-op: it still expands gõ tắt when the two
    /// switches allow it and the table is non-empty — see [`Self::process_key_english`].
    /// A host that gates English mode itself never reaches that path; one that relies
    /// on the engine to be silent while disabled must turn `shortcuts_in_english` off.
    pub fn process_key(&mut self, key: char, source: KeySource) -> ImeResult {
        if !self.session.enabled {
            return self.process_key_english(key);
        }
        if source.forces_literal_digit(key)
            || boundary::is_word_boundary(self.session.config.method, key)
        {
            return boundary::on_word_boundary(&mut self.session, key);
        }
        let (compose_key, capitalize_shortcut) = self.prepare_key(key);
        if self.session.buffer.is_empty() && compose_key.is_ascii_digit() {
            return ImeResult::none();
        }
        let raw_key = if capitalize_shortcut {
            key
        } else {
            compose_key
        };
        self.session.keys.push(raw_key);
        pipeline::process(&mut self.session, compose_key, capitalize_shortcut)
    }

    /// English mode with gõ tắt still on. Nothing is composed: the app renders
    /// every key itself, so the engine only remembers what was typed and waits for
    /// a word boundary to expand a trigger.
    ///
    /// The keys go into `buffer` as well as `keys`, because `buffer` means "what the
    /// app is showing for this word" — which here is the raw keys, and is exactly
    /// what an expansion has to delete. `prepare_key` is skipped (auto-capitalize is
    /// a Vietnamese-mode feature) and so is [`KeySource`]: a numpad digit is a
    /// literal digit in English mode either way.
    fn process_key_english(&mut self, key: char) -> ImeResult {
        if !self.session.english_shortcuts() {
            return ImeResult::none();
        }
        if boundary::is_english_boundary(key) {
            return boundary::on_english_boundary(&mut self.session, key);
        }
        self.session.keys.push(key);
        self.session.buffer.push(key);
        ImeResult::none()
    }

    fn prepare_key(&mut self, key: char) -> (char, bool) {
        if !self.session.config.auto_capitalize || !self.session.buffer.is_empty() {
            return (key, false);
        }
        let armed = self.session.cap_armed;
        self.session.cap_armed = false;
        self.session.cap_sentence_ended = false;
        if !armed {
            return (key, false);
        }
        if key.is_alphabetic() {
            return (key.to_ascii_uppercase(), false);
        }
        let shortcut = self.session.config.method.is_advanced_telex() && matches!(key, '[' | ']');
        (key, shortcut)
    }
}
