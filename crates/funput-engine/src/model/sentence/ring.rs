//! The keys behind the sentence position, newest last, so Backspace can take back
//! the one it deletes.

use funput_core::sentence::{Rules, Scanner};

/// How many keys Backspace can take back. Past that, rewinding stops: a missing
/// capital costs one Shift, and a wrong one has to be deleted.
pub(super) const DEPTH: usize = 64;

/// One key, and the scanner as it stood before the engine saw that key.
#[derive(Clone, Copy, PartialEq, Eq)]
pub(super) struct Step {
    pub(super) key: char,
    pub(super) before: Scanner,
}

impl Step {
    /// Fills a slot no key has reached. Never read: `count` gates every access.
    fn blank() -> Self {
        Self {
            key: '\0',
            before: Scanner::mid_text(Rules::TYPING),
        }
    }

    /// Whether `deleted` is this key as the screen shows it. Auto-capitalize
    /// writes the key through `to_ascii_uppercase`, so `V` on screen is the key `v`.
    fn shows_as(self, deleted: char) -> bool {
        self.key.eq_ignore_ascii_case(&deleted)
    }
}

/// Fixed-size ring: filled in place, so pushing a key never allocates.
#[derive(Clone)]
pub(super) struct Ring {
    steps: [Step; DEPTH],
    count: usize,
    cursor: usize,
}

impl Ring {
    pub(super) fn new() -> Self {
        Self {
            steps: [Step::blank(); DEPTH],
            count: 0,
            cursor: 0,
        }
    }

    pub(super) fn len(&self) -> usize {
        self.count
    }

    pub(super) fn clear(&mut self) {
        self.count = 0;
        self.cursor = 0;
    }

    pub(super) fn push(&mut self, step: Step) {
        self.steps[self.cursor] = step;
        self.cursor = (self.cursor + 1) % DEPTH;
        self.count = (self.count + 1).min(DEPTH);
    }

    /// Take the newest step back when `deleted` is its key. Any other character
    /// means the ring no longer mirrors the screen — a composed `ệ` or a gõ tắt
    /// expansion was deleted — so it is dropped: a later match would be chance.
    pub(super) fn pop_if(&mut self, deleted: char) -> Option<Scanner> {
        if self.count == 0 {
            return None;
        }
        let newest = (self.cursor + DEPTH - 1) % DEPTH;
        let step = self.steps[newest];
        if !step.shows_as(deleted) {
            self.clear();
            return None;
        }
        self.cursor = newest;
        self.count -= 1;
        Some(step.before)
    }

    /// The steps still held, oldest first.
    pub(super) fn live(&self) -> impl Iterator<Item = Step> + '_ {
        let oldest = (self.cursor + DEPTH - self.count) % DEPTH;
        (0..self.count).map(move |offset| self.steps[(oldest + offset) % DEPTH])
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn step(key: char) -> Step {
        Step {
            key,
            before: Scanner::mid_text(Rules::TYPING),
        }
    }

    fn keys(ring: &Ring) -> String {
        ring.live().map(|step| step.key).collect()
    }

    #[test]
    fn pops_the_newest_key_only() {
        let mut ring = Ring::new();
        ring.push(step('a'));
        ring.push(step('b'));

        assert!(ring.pop_if('b').is_some());
        assert_eq!(keys(&ring), "a");
    }

    #[test]
    fn an_auto_capitalized_key_matches_its_capital() {
        let mut ring = Ring::new();
        ring.push(step('v'));

        assert!(ring.pop_if('V').is_some());
        assert_eq!(ring.len(), 0);
    }

    #[test]
    fn a_mismatch_drops_every_key() {
        let mut ring = Ring::new();
        ring.push(step('a'));
        ring.push(step('a'));

        assert!(ring.pop_if('â').is_none());
        assert_eq!(ring.len(), 0);
        assert!(
            ring.pop_if('a').is_none(),
            "the older `a` is not on screen any more"
        );
    }

    #[test]
    fn wrapping_keeps_the_newest_depth_keys_in_order() {
        let mut ring = Ring::new();
        let typed: String = ('a'..='z').cycle().take(DEPTH + 3).collect();
        for key in typed.chars() {
            ring.push(step(key));
        }

        assert_eq!(ring.len(), DEPTH);
        assert_eq!(keys(&ring), typed[3..]);
    }
}
