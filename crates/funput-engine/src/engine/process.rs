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
    pub fn process_key(&mut self, key: char, source: KeySource) -> ImeResult {
        if !self.session.enabled {
            return ImeResult::none();
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
