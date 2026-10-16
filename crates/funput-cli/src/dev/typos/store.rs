//! The platform's word store, or the absence of one.
//!
//! A real keyboard ranks candidates against what the user has actually typed plus
//! the shipped syllable bands, and vetoes anything that is already a word. The
//! harness has to do the same or its numbers describe a keyboard nobody ships.

use funput_engine::CorrectionCandidate;
use funput_suggestions::{SuggestionConfig, SuggestionEngine, syllable_tier, syllable_weight};

/// Whether the harness gives the engine a word store to rank with.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev) enum Prior {
    /// None at all — what a host without a personal store would see, and the floor
    /// this feature can be measured at.
    Uniform,
    /// Every syllable of the corpus, learned once. The optimistic bound: a store
    /// that happens to know every word being typed.
    Corpus,
    /// The four bands Funput actually ships (`funput_suggestions::syllable_weight`),
    /// over an otherwise empty personal store. The realistic one, and the only
    /// number worth quoting — a fresh install has typed nothing yet.
    Shipped,
}

/// The platform's word store, or the absence of one.
pub(super) struct Store {
    prior: Prior,
    inner: Option<SuggestionEngine>,
}

impl Store {
    pub(super) fn learn(syllables: &[String], prior: Prior) -> Self {
        let inner = match prior {
            Prior::Uniform => None,
            // The bands live in the library, not in the store: an empty engine is
            // exactly what a device has on day one, and it still carries the keep
            // list the keyboards veto with.
            Prior::Shipped => Some(SuggestionEngine::in_memory(SuggestionConfig::default())),
            Prior::Corpus => {
                let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
                    max_words: syllables.len().max(1),
                    ..SuggestionConfig::default()
                });
                for syllable in syllables {
                    engine.learn(syllable);
                }
                Some(engine)
            }
        };
        Self { prior, inner }
    }

    /// The `uses` a keyboard hands the engine for `word`: the shipped band's weight
    /// plus whatever the personal store has counted.
    pub(super) fn uses(&self, word: &str) -> u32 {
        let shipped = match self.prior {
            Prior::Shipped => syllable_weight(word),
            Prior::Uniform | Prior::Corpus => 0,
        };
        shipped + self.inner.as_ref().map_or(0, |store| store.frequency(word))
    }

    /// The veto: whether `word` is already a word, the way `is_known_word` judges it.
    pub(super) fn knows(&self, word: &str) -> bool {
        self.inner
            .as_ref()
            .is_some_and(|store| store.is_known_word(word))
    }

    /// Whether `--known-only` lets `word` be offered: on the shipped list, or known
    /// to the store.
    fn admits(&self, word: &str) -> bool {
        (self.prior == Prior::Shipped && syllable_tier(word).is_some()) || self.knows(word)
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
            candidate.edits() <= max_edits && (!known_only || store.admits(candidate.text()))
        })
        .collect();
    (uses, allowed)
}
