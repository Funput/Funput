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
//! - `noise` — the key grid and the finger that misses it.
//! - `report` — what is counted, and the human and JSON renderings.
//! - `store` — the platform's word store, or the absence of one.

mod attempt;
mod noise;
mod report;
mod store;

use std::path::Path;

use funput_core::InputMethod;

use super::coverage::corpus::load_syllables;
use super::encode;
use attempt::attempt;
pub(in crate::dev) use attempt::run_keep;
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

/// Type each word through its keys and tally the outcomes.
fn measure_typed(words: &[String], keys: &[String], options: &Options) -> Tally {
    let mut rng = Rng::new(options.seed);
    let store = Store::learn(words, options.prior);
    let mut tally = Tally::default();
    for (word, keys) in words.iter().zip(keys) {
        let outcome = attempt(word, keys, options, &store, &mut rng);
        tally.add(outcome);
        if tally.samples.len() < options.show && outcome != Outcome::Typed {
            tally.samples.push((word.clone(), keys.clone(), outcome));
        }
    }
    tally
}

#[cfg(test)]
mod tests;
