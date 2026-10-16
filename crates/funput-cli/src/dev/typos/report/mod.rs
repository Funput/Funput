//! What the harness counts, and how it shows the count.
//!
//! The one number that decides the feature is `wrong`: a correction that rewrites a
//! word into something the user never typed is worse than leaving a broken one alone.

mod faults;
mod json;
mod print;

use super::inject::Fault;
pub(in crate::dev::typos) use faults::Cell;
pub(in crate::dev::typos) use json::print_json;
pub(in crate::dev::typos) use print::print_human;

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
    /// A key missed and the word stayed broken, for the reason given.
    Missed(Miss),
    /// A key missed and landed on another real syllable. Correction must not touch
    /// this — a valid word is what the user meant, as far as the engine can tell.
    Homophone,
    /// No key missed, and correction rewrote the word anyway — a deliberate word
    /// replaced. Worse than [`Outcome::Wrong`]: the user did nothing wrong.
    Rewritten,
}

/// Why a broken word stayed broken — each one points at a different knob.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev) enum Miss {
    /// The search reached no syllable.
    NoCandidate,
    /// The word store knows the broken word, so it was left as typed.
    Vetoed,
    /// The top two candidates were within Δ of each other.
    Ambiguous,
    /// No candidate beat the word as typed.
    AsTyped,
    /// The store refused every candidate.
    Refused,
}

#[derive(Default)]
pub(super) struct Tally {
    pub(super) typed: usize,
    pub(super) fixed: usize,
    pub(super) wrong: usize,
    /// Misses by reason, in [`Miss`] order.
    pub(super) missed: [usize; 5],
    pub(super) homophone: usize,
    pub(super) rewritten: usize,
    /// The same outcomes again, split by what went wrong with the word: index 0 is
    /// a touch slip with no injected fault, then one row per [`Fault`] in order.
    pub(super) by_fault: [Cell; 5],
    pub(super) samples: Vec<(String, String, Outcome)>,
}

impl Tally {
    pub(super) fn add(&mut self, outcome: Outcome, fault: Option<Fault>) {
        match outcome {
            Outcome::Typed => self.typed += 1,
            Outcome::Fixed => self.fixed += 1,
            Outcome::Wrong => self.wrong += 1,
            Outcome::Missed(miss) => self.missed[miss as usize] += 1,
            Outcome::Homophone => self.homophone += 1,
            Outcome::Rewritten => self.rewritten += 1,
        }
        self.by_fault[faults::index(fault)].add(outcome);
    }

    /// Fold another run's counts into this one — the per-seed tallies of one run.
    pub(super) fn merge(&mut self, other: Tally) {
        self.typed += other.typed;
        self.fixed += other.fixed;
        self.wrong += other.wrong;
        self.homophone += other.homophone;
        self.rewritten += other.rewritten;
        for (mine, theirs) in self.missed.iter_mut().zip(other.missed) {
            *mine += theirs;
        }
        for (mine, theirs) in self.by_fault.iter_mut().zip(other.by_fault) {
            mine.merge(theirs);
        }
        self.samples.extend(other.samples);
    }

    /// Words that were not typed as meant — a key missed, or a fault was injected —
    /// which is everything the feature could act on.
    pub(super) fn slips(&self) -> usize {
        self.fixed + self.wrong + self.missed() + self.homophone
    }

    pub(super) fn missed(&self) -> usize {
        self.missed.iter().sum()
    }

    pub(super) fn corrections(&self) -> usize {
        self.fixed + self.wrong + self.rewritten
    }
}
