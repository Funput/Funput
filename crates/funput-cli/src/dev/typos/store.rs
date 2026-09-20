//! The platform's word store, or the absence of one.
//!
//! A real keyboard ranks candidates against what the user has actually typed, and
//! vetoes anything that is already a word. The harness has to do the same or its
//! numbers describe a keyboard nobody ships.

use funput_engine::CorrectionCandidate;
use funput_suggestions::{SuggestionConfig, SuggestionEngine};

/// The floor `funput-engine` gives a word nobody has typed, repeated here because
/// the harness has to reproduce the engine's decision to vary the margin around it.
const PRIOR_FLOOR: f32 = 0.5;

/// Whether the harness gives the engine a word store to rank with.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev) enum Prior {
    /// None at all — what a host without a personal store would see, and the floor
    /// this feature can be measured at.
    Uniform,
    /// Every syllable of the corpus, learned once. Stands in for the shipped
    /// Vietnamese word list: a structurally valid non-word scores zero, a real one
    /// does not.
    Corpus,
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
        let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
            max_words: syllables.len().max(1),
            ..SuggestionConfig::default()
        });
        for syllable in syllables {
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

/// The platform's decision, reproduced here so the confidence margin can be varied
/// without rebuilding the engine.
///
/// Identical to `Engine::choose_correction` at `margin = 1.0` with `known_only` off
/// — which the test below pins — and that is the point: the sweep measures the
/// engine's own rule, not a second one written for the occasion.
pub(super) fn decide(
    candidates: &[CorrectionCandidate],
    store: &Store,
    margin: f32,
    known_only: bool,
    max_edits: usize,
) -> Option<usize> {
    let mut best: Option<(usize, f32)> = None;
    let mut runner_up = f32::NEG_INFINITY;
    for (index, candidate) in candidates.iter().enumerate() {
        if candidate.edits() > max_edits || (known_only && !store.knows(candidate.text())) {
            continue;
        }
        let score = candidate.touch_score()
            + (1.0 + store.uses(candidate.text()) as f32).ln()
            + PRIOR_FLOOR;
        match best {
            Some((_, leader)) if leader >= score => runner_up = runner_up.max(score),
            Some((_, leader)) => {
                runner_up = runner_up.max(leader);
                best = Some((index, score));
            }
            None => best = Some((index, score)),
        }
    }
    let (index, top) = best?;
    (top - runner_up >= margin).then_some(index)
}
