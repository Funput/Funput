//! Sentence position for auto-capitalize, plus the keys behind it so Backspace can
//! take back the one it deletes.

mod ring;

use std::fmt;

use funput_core::sentence::{Rules, Scanner};

use ring::{Ring, Step};

/// The scanner auto-capitalize reads, and the [`Ring`] of keys that moved it. The
/// ring is boxed: allocated with the engine, so a keystroke does not allocate and
/// [`Engine`](crate::Engine) only grows by the pointer.
#[derive(Clone)]
pub(crate) struct SentenceTrace {
    scanner: Scanner,
    ring: Box<Ring>,
}

impl SentenceTrace {
    /// Caret position unknown: nothing is capitalized until a sentence end is typed.
    pub(crate) fn mid_text() -> Self {
        Self {
            scanner: Scanner::mid_text(Rules::TYPING),
            ring: Box::new(Ring::new()),
        }
    }

    /// Document start. Drops keys typed before the caret arrived.
    pub(crate) fn arm(&mut self) {
        self.scanner = Scanner::new(Rules::TYPING);
        self.ring.clear();
    }

    pub(crate) fn disarm(&mut self) {
        self.scanner = Scanner::mid_text(Rules::TYPING);
        self.ring.clear();
    }

    /// Keep the position but stop matching Backspace against the keys so far:
    /// what comes next reaches the screen without passing through here.
    pub(crate) fn forget_keys(&mut self) {
        self.ring.clear();
    }

    pub(crate) fn snapshot(&self) -> Scanner {
        self.scanner
    }

    pub(crate) fn awaiting_sentence(&self) -> bool {
        self.scanner.awaiting_sentence()
    }

    pub(crate) fn consume_sentence_start(&mut self) {
        self.scanner.consume_sentence_start();
    }

    /// Feed `key`. `before` is the scanner from before the engine saw the key,
    /// which can differ from the current one when the key opened a sentence.
    pub(crate) fn push(&mut self, key: char, before: Scanner) {
        self.ring.push(Step { key, before });
        self.scanner.push(key);
    }

    /// Undo `deleted` when it is the newest key, so a composed `ệ` stays put.
    /// Returns whether anything was undone.
    pub(crate) fn rewind(&mut self, deleted: char) -> bool {
        let Some(before) = self.ring.pop_if(deleted) else {
            return false;
        };
        self.scanner = before;
        true
    }
}

impl PartialEq for SentenceTrace {
    fn eq(&self, other: &Self) -> bool {
        self.scanner == other.scanner && self.ring.live().eq(other.ring.live())
    }
}

impl Eq for SentenceTrace {}

impl fmt::Debug for SentenceTrace {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("SentenceTrace")
            .field("scanner", &self.scanner)
            .field("keys", &self.ring.len())
            .finish()
    }
}

#[cfg(test)]
mod tests {
    use super::ring::DEPTH;
    use super::*;

    /// What `Engine::process_key` does: snapshot, maybe open the sentence, push.
    fn push(trace: &mut SentenceTrace, key: char) {
        let before = trace.snapshot();
        if trace.awaiting_sentence() && key.is_alphabetic() {
            trace.consume_sentence_start();
        }
        trace.push(key, before);
    }

    fn typed(keys: &str) -> SentenceTrace {
        let mut trace = SentenceTrace::mid_text();
        for key in keys.chars() {
            push(&mut trace, key);
        }
        trace
    }

    #[test]
    fn rewinding_a_full_stop_leaves_the_next_space_unconfirmed() {
        let mut trace = typed("ok.");

        assert!(trace.rewind('.'));
        push(&mut trace, ' ');

        assert!(!trace.awaiting_sentence());
    }

    #[test]
    fn a_different_character_does_not_rewind() {
        let mut trace = typed("ok.");

        assert!(!trace.rewind('x'));
        push(&mut trace, ' ');

        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn rewinding_the_confirming_space_unarms_the_sentence() {
        let mut trace = typed("ok. ");
        assert!(trace.awaiting_sentence());

        assert!(trace.rewind(' '));

        assert!(!trace.awaiting_sentence());
    }

    #[test]
    fn rewinding_the_capitalized_opening_letter_reopens_the_sentence() {
        let mut trace = SentenceTrace::mid_text();
        trace.arm();
        push(&mut trace, 'v');
        assert!(!trace.awaiting_sentence());

        assert!(trace.rewind('V'), "the screen shows the capital");

        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn arming_drops_keys_typed_before_the_caret_arrived() {
        let mut trace = typed(".");

        trace.arm();

        assert!(!trace.rewind('.'));
        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn forgetting_keys_keeps_the_position() {
        let mut trace = typed("ok. ");

        trace.forget_keys();

        assert!(!trace.rewind(' '));
        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn past_the_ring_backspace_stops_instead_of_guessing() {
        let mut trace = typed(".");
        for _ in 0..DEPTH {
            push(&mut trace, 'a');
        }

        for _ in 0..DEPTH {
            assert!(trace.rewind('a'));
        }
        assert!(!trace.rewind('a'));
        // The full stop itself fell out of the ring, but the state it left — a
        // terminator waiting for its space — is what the oldest `a` restored.
        push(&mut trace, ' ');
        assert!(trace.awaiting_sentence());
    }
}
