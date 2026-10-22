//! Sentence position for auto-capitalize, plus the last [`DEPTH`] keys so
//! Backspace can undo one it can still name. Past that, rewinding stops: a
//! missing capital costs one Shift, and a wrong one has to be deleted.

use std::fmt;

use funput_core::sentence::{Rules, Scanner};

/// Same horizon as the desktop shell's committed-text shadow.
const DEPTH: usize = 64;

#[derive(Clone, Copy, PartialEq, Eq)]
struct Step {
    key: char,
    before: Scanner,
}

impl Step {
    fn blank() -> Self {
        Self {
            key: '\0',
            before: Scanner::mid_text(Rules::TYPING),
        }
    }
}

/// Boxed ring: allocated with the engine, so a keystroke does not allocate and
/// [`Engine`](crate::Engine) only grows by the pointer.
#[derive(Clone)]
pub(crate) struct SentenceTrace {
    scanner: Scanner,
    ring: Box<Ring>,
}

#[derive(Clone)]
struct Ring {
    steps: [Step; DEPTH],
    count: usize,
    cursor: usize,
}

impl SentenceTrace {
    /// Caret position unknown: nothing is capitalized until a sentence end is typed.
    pub(crate) fn mid_text() -> Self {
        Self::at(Scanner::mid_text(Rules::TYPING))
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

    pub(crate) fn snapshot(&self) -> Scanner {
        self.scanner
    }

    pub(crate) fn awaiting_sentence(&self) -> bool {
        self.scanner.awaiting_sentence()
    }

    pub(crate) fn consume_sentence_start(&mut self) {
        self.scanner.consume_sentence_start();
    }

    pub(crate) fn push(&mut self, key: char, before: Scanner) {
        self.ring.push(Step { key, before });
        self.scanner.push(key);
    }

    /// Undo `deleted` only when it is the last key, so a composed `ệ` stays put.
    pub(crate) fn rewind(&mut self, deleted: char) -> bool {
        self.ring
            .pop_if(deleted)
            .inspect(|before| self.scanner = *before)
            .is_some()
    }

    fn at(scanner: Scanner) -> Self {
        Self {
            scanner,
            ring: Box::new(Ring::new()),
        }
    }
}

impl Ring {
    fn new() -> Self {
        Self {
            steps: [Step::blank(); DEPTH],
            count: 0,
            cursor: 0,
        }
    }

    fn clear(&mut self) {
        self.count = 0;
        self.cursor = 0;
    }

    fn push(&mut self, step: Step) {
        self.steps[self.cursor] = step;
        self.cursor = (self.cursor + 1) % DEPTH;
        if self.count < DEPTH {
            self.count += 1;
        }
    }

    fn pop_if(&mut self, deleted: char) -> Option<Scanner> {
        if self.count == 0 {
            return None;
        }
        let index = (self.cursor + DEPTH - 1) % DEPTH;
        let step = self.steps[index];
        if step.key != deleted {
            return None;
        }
        self.cursor = index;
        self.count -= 1;
        Some(step.before)
    }

    fn live(&self) -> impl Iterator<Item = Step> + '_ {
        let start = (self.cursor + DEPTH - self.count) % DEPTH;
        (0..self.count).map(move |offset| self.steps[(start + offset) % DEPTH])
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
            .field("count", &self.ring.count)
            .finish()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn push(trace: &mut SentenceTrace, key: char) {
        let before = trace.snapshot();
        // A sentence start is consumed before the key is pushed, and the snapshot
        // has to be the scanner from before that.
        if trace.awaiting_sentence() && key.is_alphabetic() {
            trace.consume_sentence_start();
        }
        trace.push(key, before);
    }

    #[test]
    fn rewinding_a_full_stop_leaves_the_next_space_unconfirmed() {
        let mut trace = SentenceTrace::mid_text();
        for key in "ok.".chars() {
            push(&mut trace, key);
        }

        assert!(trace.rewind('.'));
        push(&mut trace, ' ');

        assert!(!trace.awaiting_sentence());
    }

    #[test]
    fn a_different_character_does_not_rewind() {
        let mut trace = SentenceTrace::mid_text();
        for key in "ok.".chars() {
            push(&mut trace, key);
        }

        assert!(!trace.rewind('x'));
        push(&mut trace, ' ');

        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn rewinding_the_confirming_space_unarms_the_sentence() {
        let mut trace = SentenceTrace::mid_text();
        for key in "ok. ".chars() {
            push(&mut trace, key);
        }
        assert!(trace.awaiting_sentence());

        assert!(trace.rewind(' '));

        assert!(!trace.awaiting_sentence());
    }

    #[test]
    fn rewinding_the_opening_letter_restores_a_consumed_sentence_start() {
        let mut trace = SentenceTrace::mid_text();
        trace.arm();
        push(&mut trace, 'v');
        assert!(!trace.awaiting_sentence());

        assert!(trace.rewind('v'));

        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn arming_drops_keys_typed_before_the_caret_arrived() {
        let mut trace = SentenceTrace::mid_text();
        push(&mut trace, '.');

        trace.arm();

        assert!(!trace.rewind('.'));
        assert!(trace.awaiting_sentence());
    }

    #[test]
    fn past_the_ring_backspace_stops_instead_of_guessing() {
        let mut trace = SentenceTrace::mid_text();
        push(&mut trace, '.');
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
