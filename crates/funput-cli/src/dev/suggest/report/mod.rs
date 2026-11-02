//! What a replay counts, and the two ways to print it.

mod json;
mod print;
mod sweep;

use std::collections::HashMap;

pub(super) use json::print_json;
pub(super) use print::print_human;
pub(super) use sweep::{print_sweep_header, print_sweep_row};

/// Which data a Predict came from. Told apart by a twin engine with the shipped
/// data off: if the twin stays silent where the real engine speaks, the word
/// came from the shipped pair table.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub(super) enum Source {
    Shipped,
    Personal,
}

impl Source {
    pub(super) fn name(self) -> &'static str {
        match self {
            Self::Shipped => "shipped",
            Self::Personal => "personal",
        }
    }

    fn index(self) -> usize {
        self as usize
    }
}

/// A wrong Predict: previous word, guess, word typed instead, source.
pub(super) type Wrong = (String, String, String, Source);
/// A silent Predict: previous word, word typed.
pub(super) type Silent = (String, String);

/// Counts from one replay.
#[derive(Debug, Default)]
pub(super) struct Tally {
    /// Words replayed.
    pub(super) words: usize,
    /// Keystrokes the words take to type in full.
    pub(super) keys: usize,
    /// Keystrokes a user who always takes a right suggestion would skip.
    pub(super) saved: usize,
    /// Words typed right after a space that followed a word: Predict was asked.
    pub(super) predict_chances: usize,
    /// Predicts that were the next word, by [`Source`].
    pub(super) predict_right: [usize; 2],
    /// Predicts that were not the next word, by [`Source`].
    pub(super) predict_wrong: [usize; 2],
    /// Words long enough to complete (3+ letters) that Predict did not already give.
    pub(super) completion_words: usize,
    /// Of those, the words that showed up in the bar before they were finished.
    pub(super) completion_found: usize,
    /// Sum of the prefix lengths (in letters) at which they showed up.
    pub(super) completion_prefix_letters: usize,
    /// How many of the three slots each completion query filled.
    pub(super) bar: [usize; 4],
    /// Wrong Predicts: (previous word, guess, word typed, source) → times.
    pub(super) wrong: HashMap<Wrong, usize>,
    /// Silent Predicts: (previous word, word typed) → times.
    pub(super) silent: HashMap<Silent, usize>,
}

impl Tally {
    pub(super) fn predict_hit(&mut self, source: Source) {
        self.predict_right[source.index()] += 1;
    }

    pub(super) fn predict_miss(&mut self, source: Source, previous: &str, guess: &str, word: &str) {
        self.predict_wrong[source.index()] += 1;
        let key = (
            previous.to_owned(),
            guess.to_owned(),
            word.to_owned(),
            source,
        );
        *self.wrong.entry(key).or_default() += 1;
    }

    pub(super) fn predict_silent(&mut self, previous: &str, word: &str) {
        *self
            .silent
            .entry((previous.to_owned(), word.to_owned()))
            .or_default() += 1;
    }

    pub(super) fn predict_shown(&self) -> usize {
        self.predict_right.iter().sum::<usize>() + self.predict_wrong.iter().sum::<usize>()
    }

    pub(super) fn predict_right_total(&self) -> usize {
        self.predict_right.iter().sum()
    }

    pub(super) fn predict_wrong_total(&self) -> usize {
        self.predict_wrong.iter().sum()
    }

    /// The `limit` most frequent wrong Predicts, most frequent first.
    pub(super) fn top_wrong(&self, limit: usize) -> Vec<(&Wrong, usize)> {
        top(&self.wrong, limit)
    }

    /// The `limit` most frequent word pairs Predict had nothing for.
    pub(super) fn top_silent(&self, limit: usize) -> Vec<(&Silent, usize)> {
        top(&self.silent, limit)
    }
}

/// Most frequent first, ties in key order, so a run prints the same every time.
fn top<K: Ord>(counts: &HashMap<K, usize>, limit: usize) -> Vec<(&K, usize)> {
    let mut rows: Vec<_> = counts.iter().map(|(key, &n)| (key, n)).collect();
    rows.sort_by(|a, b| b.1.cmp(&a.1).then_with(|| a.0.cmp(b.0)));
    rows.truncate(limit);
    rows
}

/// `part` as a percentage of `whole`, 0 when there is no whole.
pub(super) fn pct(part: usize, whole: usize) -> f64 {
    if whole == 0 {
        0.0
    } else {
        part as f64 * 100.0 / whole as f64
    }
}
