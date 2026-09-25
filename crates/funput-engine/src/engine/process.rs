use crate::compose::{boundary, pipeline};
use crate::correction::{self, StoredTouch};
use crate::{Action, Engine, ImeResult, KeySource};

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
        // A correction the platform never answered settles here instead of hanging
        // on: whatever the word boundary deferred is replayed into this keystroke.
        let deferred = correction::flush_pending(&mut self.session);
        let result = self.process_key_inner(key, source);
        merge(deferred, result, key)
    }

    fn process_key_inner(&mut self, key: char, source: KeySource) -> ImeResult {
        let touch = correction::take_next(&mut self.session);
        if !self.session.enabled {
            return self.process_key_english(key);
        }
        // Every key reaches the scanner, whatever the result below turns out to be,
        // and whether or not auto-capitalize is on: only reading it is gated, so
        // turning the switch on mid-session finds the sentence already tracked.
        // `…` arrives here too, which is how it counts as a sentence end without
        // `is_english_boundary` — that predicate also gates gõ tắt and English
        // restore, and widening it would start expanding triggers on an ellipsis.
        let result = self.compose_key(key, source, touch);
        self.session.scanner.push(key);
        let word_open = !self.session.keys.is_empty();
        self.session.glue.after_key(key, word_open);
        result
    }

    fn compose_key(
        &mut self,
        key: char,
        source: KeySource,
        touch: Option<StoredTouch>,
    ) -> ImeResult {
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
        if self.session.keys.is_empty() {
            self.session.glue.start_word();
        }
        self.session.keys.push(raw_key);
        correction::note_key(&mut self.session, raw_key, touch);
        let result = pipeline::process(&mut self.session, compose_key, capitalize_shortcut);
        // The pipeline rewrites the raw keys when a modifier is reverted, which would
        // leave the touch log describing a word that no longer exists.
        correction::verify_alignment(&mut self.session);
        result
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
        if !self.session.scanner.awaiting_sentence() {
            // Nothing to take, and taking anyway would drop a terminator still
            // waiting for its space — which is what a closer like `»` is doing here,
            // since it is not ASCII and so never reached the boundary path.
            return (key, false);
        }
        // Taken here rather than left to the push below, because `[` expanding to a
        // whole word is punctuation as far as the scan is concerned and would leave
        // the sentence open over the word it just wrote.
        self.session.scanner.consume_sentence_start();
        if key.is_alphabetic() {
            return (key.to_ascii_uppercase(), false);
        }
        let shortcut = self.session.config.method.is_advanced_telex() && matches!(key, '[' | ']');
        (key, shortcut)
    }
}

/// Fold a restore the platform never collected into this keystroke's own result.
pub(crate) fn merge(deferred: Option<ImeResult>, result: ImeResult, key: char) -> ImeResult {
    let Some(mut deferred) = deferred else {
        return result;
    };
    // The deferred edit deletes the word the previous boundary left behind. This key
    // has only just opened a new word, so it can have nothing of its own to delete.
    debug_assert_eq!(
        result.backspace, 0,
        "a flushed restore cannot absorb another backspace"
    );
    match result.action {
        // `None` means the platform echoes the key itself — but a `Send` swallows it,
        // so it has to ride along in the output.
        Action::None => deferred.output.push(key),
        _ => deferred.output.push_str(&result.output),
    }
    deferred
}
