//! What the harness counts, and the one number that decides the feature.

/// What became of one word.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev) enum Outcome {
    /// Every key landed where it was aimed, and nothing was offered.
    Typed,
    /// A key missed, and correction put the word back.
    Fixed,
    /// A key missed, and correction replaced it with a *different* word. The one
    /// outcome that is worse than doing nothing.
    Wrong,
    /// A key missed and the word stayed broken: nothing was offered, or the top two
    /// were too close to call.
    Missed,
    /// A key missed and landed on another real syllable. Correction must not touch
    /// this — a valid word is what the user meant, as far as the engine can tell.
    Homophone,
}

#[derive(Default)]
pub(super) struct Tally {
    pub(super) typed: usize,
    pub(super) fixed: usize,
    pub(super) wrong: usize,
    pub(super) missed: usize,
    pub(super) homophone: usize,
    pub(super) samples: Vec<(String, String, Outcome)>,
}

impl Tally {
    pub(super) fn add(&mut self, outcome: Outcome) {
        match outcome {
            Outcome::Typed => self.typed += 1,
            Outcome::Fixed => self.fixed += 1,
            Outcome::Wrong => self.wrong += 1,
            Outcome::Missed => self.missed += 1,
            Outcome::Homophone => self.homophone += 1,
        }
    }

    /// Words where a key actually missed — everything the feature could act on.
    pub(super) fn slips(&self) -> usize {
        self.fixed + self.wrong + self.missed + self.homophone
    }

    pub(super) fn corrections(&self) -> usize {
        self.fixed + self.wrong
    }
}
