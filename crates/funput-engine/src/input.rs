use crate::{Engine, ImeResult, boundary, pipeline};

impl Engine {
    /// Process one Unicode scalar and return the platform edit instruction.
    pub fn process_char(&mut self, key: char) -> ImeResult {
        if !self.session.enabled {
            return ImeResult::none();
        }
        if boundary::is_word_boundary(self.session.method, key) {
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
        if !self.session.auto_capitalize || !self.session.buffer.is_empty() {
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
        let shortcut = self.session.method.is_advanced_telex() && matches!(key, '[' | ']');
        (key, shortcut)
    }
}
