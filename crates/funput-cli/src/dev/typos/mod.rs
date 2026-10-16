//! `funput dev typos`: how often typo correction helps, and how often it hurts.
//!
//! Takes every syllable in a corpus, types it with a finger that misses by a
//! Gaussian σ, and counts what the engine does about it. The number that decides
//! whether the feature ships is not how many words it fixes — it is how many it
//! breaks, because a wrong correction rewrites a word the user meant.
//!
//! The engine here has **no word store**, so candidates are ranked on touch evidence
//! alone. A real keyboard adds the user's own word frequencies on top, so these
//! figures are the floor, not the expectation.
//!
//! # Layout
//!
//! - this file — the run loop.
//! - `attempt` — typing one word and naming what became of it; `--keep` mode.
//! - `inject` — the faults a fast typist makes before the finger even wanders.
//! - `noise` — the key grid and the finger that misses it.
//! - `report` — what is counted, and the human and JSON renderings.
//! - `store` — the platform's word store, or the absence of one.

mod attempt;
mod inject;
mod noise;
mod report;
mod store;

use std::path::Path;

use funput_core::InputMethod;

use super::coverage::corpus::load_syllables;
use super::encode;
use attempt::attempt;
pub(in crate::dev) use attempt::run_keep;
pub(in crate::dev) use inject::{Fault, Rates};
use noise::Rng;
use report::{Outcome, Tally};
pub(in crate::dev) use store::Prior;
use store::Store;

#[derive(Clone, Copy)]
pub(super) struct Options {
    pub(super) method: InputMethod,
    pub(super) prior: Prior,
    pub(super) noise: f32,
    /// Only offer a candidate the word store recognizes, rather than any
    /// structurally valid syllable.
    pub(super) known_only: bool,
    /// How many substituted keys a candidate may carry.
    pub(super) max_edits: usize,
    pub(super) seed: u64,
    /// How many seeds to run, from `seed` up, summing the tallies.
    pub(super) seeds: u64,
    /// How often a word gets one typing fault, and of which kind.
    pub(super) faults: Rates,
    pub(super) limit: Option<usize>,
    pub(super) show: usize,
    pub(super) json: bool,
}

/// Run the harness over `corpus_path` and print the report.
pub fn run(corpus_path: &Path, options: &Options) -> std::io::Result<()> {
    let mut syllables: Vec<String> = load_syllables(corpus_path)?.into_iter().collect();
    if let Some(limit) = options.limit {
        syllables.truncate(limit);
    }
    let tally = measure(&syllables, options);
    if options.json {
        report::print_json(corpus_path, &syllables, &tally, options);
    } else {
        report::print_human(corpus_path, &syllables, &tally, options);
    }
    Ok(())
}

fn measure(syllables: &[String], options: &Options) -> Tally {
    let keys: Vec<String> = syllables
        .iter()
        .map(|syllable| encode::encode(syllable, options.method))
        .collect();
    measure_typed(syllables, &keys, options)
}

/// Type each word through its keys, once per seed, and tally the outcomes.
fn measure_typed(words: &[String], keys: &[String], options: &Options) -> Tally {
    let store = Store::learn(words, options.prior);
    let mut tally = Tally::default();
    for seed in options.seed..options.seed + options.seeds.max(1) {
        tally.merge(measure_seed(words, keys, options, &store, seed));
    }
    tally.samples.truncate(options.show);
    tally
}

fn measure_seed(
    words: &[String],
    keys: &[String],
    options: &Options,
    store: &Store,
    seed: u64,
) -> Tally {
    let mut rng = Rng::new(seed);
    // Faults draw from their own stream, so a seed types the same touches it
    // always did — the numbers pinned in the docs stay reproducible.
    let mut faults = rng.derived(0xFA17);
    let mut tally = Tally::default();
    for (word, keys) in words.iter().zip(keys) {
        let typed = attempt(word, keys, options, store, &mut rng, &mut faults);
        tally.add(typed.outcome, typed.fault);
        if tally.samples.len() < options.show && typed.outcome != Outcome::Typed {
            tally
                .samples
                .push((word.clone(), typed.keys, typed.outcome));
        }
    }
    tally
}

#[cfg(test)]
mod tests;
