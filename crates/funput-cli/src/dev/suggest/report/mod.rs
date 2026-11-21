//! What a replay counts, and the two ways to print it.

mod json;
mod predict;
mod print;
mod sweep;

pub(super) use crate::dev::render::pct;
pub(super) use json::print_json;
pub(super) use predict::{Predicts, Source};
pub(super) use print::print_human;
pub(super) use sweep::{print_sweep_header, print_sweep_row};

/// Counts from one replay.
#[derive(Debug, Default)]
pub(super) struct Tally {
    /// Words replayed.
    pub(super) words: usize,
    /// Keystrokes the words take to type in full.
    pub(super) keys: usize,
    /// Keystrokes a user who always takes a right suggestion would skip.
    pub(super) saved: usize,
    /// The bars Predict was asked for, right after a space.
    pub(super) predict: Predicts,
    /// Words long enough to complete (3+ letters) that Predict did not already give.
    pub(super) completion_words: usize,
    /// Of those, the words that showed up in the bar before they were finished.
    pub(super) completion_found: usize,
    /// Sum of the prefix lengths (in letters) at which they showed up.
    pub(super) completion_prefix_letters: usize,
    /// How many of the three slots each completion query filled.
    pub(super) bar: [usize; 4],
}
