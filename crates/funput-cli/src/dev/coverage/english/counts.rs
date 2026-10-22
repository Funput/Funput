//! Typing the lexicon under one setup and counting each kind of wrong word.

use funput_core::InputMethod;

use super::outcome::{Outcome, classify, words};
use super::{SETUPS, Setup};
use crate::dev::sim::{SimConfig, simulate_with};

/// How one setup did, with up to `keep` wrong words of each kind to show.
pub(super) struct Counts {
    pub(super) label: &'static str,
    pub(super) total: usize,
    pub(super) lost_key: usize,
    pub(super) vietnamese: usize,
    pub(super) other: usize,
    pub(super) samples: Vec<(String, String, Outcome)>,
}

impl Counts {
    pub(super) fn wrong(&self) -> usize {
        self.lost_key + self.vietnamese + self.other
    }

    /// Every count under the name the baseline gives it.
    pub(super) fn entries(&self) -> [(String, usize); 5] {
        let name = |what: &str| format!("{}.{what}", self.label);
        [
            (name("total"), self.total),
            (name("wrong"), self.wrong()),
            (name("lost_key"), self.lost_key),
            (name("vietnamese"), self.vietnamese),
            (name("other"), self.other),
        ]
    }
}

pub(super) fn evaluate(words: &[&str], setup: Setup, keep: usize) -> Counts {
    let config = SimConfig {
        eager_restore: setup.eager_restore,
        ..SimConfig::new(InputMethod::Telex)
    };
    let mut counts = Counts {
        label: setup.label,
        total: words.len(),
        lost_key: 0,
        vietnamese: 0,
        other: 0,
        samples: Vec::new(),
    };
    for word in words {
        let shown = simulate_with(config, &format!("{word} ")).app_text;
        let produced = shown.strip_suffix(' ').unwrap_or(&shown);
        let outcome = classify(word, produced);
        match outcome {
            Outcome::Typed => continue,
            Outcome::LostKey => counts.lost_key += 1,
            Outcome::Vietnamese => counts.vietnamese += 1,
            Outcome::Other => counts.other += 1,
        }
        // Up to `keep` of each kind, so the rare lost key is not crowded out.
        if counts
            .samples
            .iter()
            .filter(|sample| sample.2 == outcome)
            .count()
            < keep
        {
            counts
                .samples
                .push(((*word).to_owned(), produced.to_owned(), outcome));
        }
    }
    counts
}

/// Every setup over the words of `tsv`.
pub(super) fn evaluate_all(tsv: &str, limit: Option<usize>, keep: usize) -> Vec<Counts> {
    let mut words = words(tsv);
    words.truncate(limit.unwrap_or(usize::MAX));
    SETUPS
        .iter()
        .map(|&setup| evaluate(&words, setup, keep))
        .collect()
}
