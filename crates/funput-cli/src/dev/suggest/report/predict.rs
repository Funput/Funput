//! What a replay counts about Predict: the bar right after a space, which may
//! hold up to three words.

use std::collections::HashMap;

/// Which data a Predict came from. Told apart by a twin engine with the shipped
/// data off: a word the twin does not offer where the real engine does came
/// from the shipped pair table.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub(in crate::dev::suggest) enum Source {
    Shipped,
    Personal,
}

impl Source {
    /// Every source, in the order reports list them.
    pub(in crate::dev::suggest) const ALL: [Source; 2] = [Source::Shipped, Source::Personal];

    pub(in crate::dev::suggest) fn name(self) -> &'static str {
        match self {
            Self::Shipped => "shipped",
            Self::Personal => "personal",
        }
    }

    fn index(self) -> usize {
        self as usize
    }
}

/// A wrong Predict: previous word, what the bar held, word typed instead, source.
pub(in crate::dev::suggest) type Wrong = (String, String, String, Source);
/// A silent Predict: previous word, word typed.
pub(in crate::dev::suggest) type Silent = (String, String);

/// Counts of the bars Predict was asked for.
#[derive(Debug, Default)]
pub(in crate::dev::suggest) struct Predicts {
    /// Words typed right after a space that followed a word: Predict was asked.
    pub(in crate::dev::suggest) chances: usize,
    /// Bars that held the next word in any slot, by [`Source`].
    pub(in crate::dev::suggest) right: [usize; 2],
    /// Bars that did not hold the next word, by [`Source`].
    pub(in crate::dev::suggest) wrong: [usize; 2],
    /// Of the right bars, those whose first slot was the next word.
    pub(in crate::dev::suggest) right_first: usize,
    /// Slots filled across every bar shown.
    pub(in crate::dev::suggest) slots: usize,
    /// Wrong bars: (previous word, bar, word typed, source) → times.
    misses: HashMap<Wrong, usize>,
    /// Silent bars: (previous word, word typed) → times.
    silences: HashMap<Silent, usize>,
}

impl Predicts {
    /// A bar of `slots` words that held the next word at `slot`.
    pub(in crate::dev::suggest) fn hit(&mut self, source: Source, slot: usize, slots: usize) {
        self.chances += 1;
        self.slots += slots;
        self.right[source.index()] += 1;
        if slot == 0 {
            self.right_first += 1;
        }
    }

    /// A bar that held `bar` where `word` was typed after `previous`.
    pub(in crate::dev::suggest) fn miss(
        &mut self,
        source: Source,
        previous: &str,
        bar: &[String],
        word: &str,
    ) {
        self.chances += 1;
        self.slots += bar.len();
        self.wrong[source.index()] += 1;
        let key = (previous.to_owned(), bar.join(", "), word.to_owned(), source);
        *self.misses.entry(key).or_default() += 1;
    }

    /// A bar that stayed empty where `word` was typed after `previous`.
    pub(in crate::dev::suggest) fn silent(&mut self, previous: &str, word: &str) {
        self.chances += 1;
        *self
            .silences
            .entry((previous.to_owned(), word.to_owned()))
            .or_default() += 1;
    }

    /// (right, shown) for the bars that came from `source`.
    pub(in crate::dev::suggest) fn by_source(&self, source: Source) -> (usize, usize) {
        let i = source.index();
        (self.right[i], self.right[i] + self.wrong[i])
    }

    pub(in crate::dev::suggest) fn shown(&self) -> usize {
        self.right_total() + self.wrong_total()
    }

    pub(in crate::dev::suggest) fn right_total(&self) -> usize {
        self.right.iter().sum()
    }

    pub(in crate::dev::suggest) fn wrong_total(&self) -> usize {
        self.wrong.iter().sum()
    }

    /// The `limit` most frequent wrong bars, most frequent first.
    pub(in crate::dev::suggest) fn top_wrong(&self, limit: usize) -> Vec<(&Wrong, usize)> {
        top(&self.misses, limit)
    }

    /// The `limit` most frequent word pairs Predict had nothing for.
    pub(in crate::dev::suggest) fn top_silent(&self, limit: usize) -> Vec<(&Silent, usize)> {
        top(&self.silences, limit)
    }
}

/// Most frequent first, ties in key order, so a run prints the same every time.
fn top<K: Ord>(counts: &HashMap<K, usize>, limit: usize) -> Vec<(&K, usize)> {
    let mut rows: Vec<_> = counts.iter().map(|(key, &n)| (key, n)).collect();
    rows.sort_by(|a, b| b.1.cmp(&a.1).then_with(|| a.0.cmp(b.0)));
    rows.truncate(limit);
    rows
}
