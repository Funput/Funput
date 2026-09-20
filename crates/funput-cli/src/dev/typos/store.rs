//! The platform's word store, or the absence of one.
//!
//! A real keyboard ranks candidates against what the user has actually typed, and
//! vetoes anything that is already a word. The harness has to do the same or its
//! numbers describe a keyboard nobody ships.

use funput_engine::CorrectionCandidate;
use funput_suggestions::{SuggestionConfig, SuggestionEngine};

/// Whether the harness gives the engine a word store to rank with.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev) enum Prior {
    /// None at all — what a host without a personal store would see, and the floor
    /// this feature can be measured at.
    Uniform,
    /// Every syllable of the corpus, learned once. The optimistic bound: a store
    /// that happens to know every word being typed.
    Corpus,
    /// The list Funput actually ships. The realistic one, and the only number worth
    /// quoting — it is deliberately much smaller than any corpus.
    Shipped,
}

/// The syllable list the platforms bundle, so the harness measures what ships.
const SHIPPED: &str = include_str!("../../../../funput-suggestions/data/syllables/vi.txt");

fn shipped_syllables() -> Vec<&'static str> {
    SHIPPED
        .lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .collect()
}

/// The platform's word store, or the absence of one.
pub(super) struct Store {
    inner: Option<SuggestionEngine>,
}

impl Store {
    pub(super) fn learn(syllables: &[String], prior: Prior) -> Self {
        if prior == Prior::Uniform {
            return Self { inner: None };
        }
        let shipped = shipped_syllables();
        let words: Vec<&str> = match prior {
            Prior::Shipped => shipped,
            _ => syllables.iter().map(String::as_str).collect(),
        };
        let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
            max_words: words.len().max(1),
            ..SuggestionConfig::default()
        });
        for syllable in &words {
            engine.learn(syllable);
        }
        Self {
            inner: Some(engine),
        }
    }

    pub(super) fn uses(&self, word: &str) -> u32 {
        self.inner.as_ref().map_or(0, |store| store.frequency(word))
    }

    pub(super) fn knows(&self, word: &str) -> bool {
        self.inner
            .as_ref()
            .is_some_and(|store| store.is_known_word(word))
    }
}

/// The two arrays the engine ranks with: how often the user typed each candidate,
/// and whether the store recognizes it as a word at all.
pub(super) fn ballots(
    candidates: &[CorrectionCandidate],
    store: &Store,
    known_only: bool,
    max_edits: usize,
) -> (Vec<u32>, Vec<bool>) {
    let uses = candidates
        .iter()
        .map(|candidate| store.uses(candidate.text()))
        .collect();
    let allowed = candidates
        .iter()
        .map(|candidate| {
            candidate.edits() <= max_edits && (!known_only || store.knows(candidate.text()))
        })
        .collect();
    (uses, allowed)
}
