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
//! - this file — the run loop and how each word is classified.
//! - `noise` — the key grid and the finger that misses it.
//! - `report` — the human and JSON renderings.

mod noise;
mod report;
mod store;

use std::path::Path;

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use super::coverage::corpus::load_syllables;
use super::encode::encode;
use noise::{Rng, aim};
use report::{Outcome, Tally};
pub(in crate::dev) use store::Prior;
use store::{Store, decide};

/// Type `syllable` with a wandering finger and report what correction made of it.
///
/// `store` stands in for the platform's word store, and is used exactly the way a
/// keyboard uses it: veto first, then weigh the candidates.
fn attempt(syllable: &str, options: &Options, store: &Store, rng: &mut Rng) -> Outcome {
    let (method, noise) = (options.method, options.noise);
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.typo_correction = true;
    });
    let mut app = String::new();
    let mut slipped = false;
    for key in encode(syllable, method).chars() {
        let (hit, touch) = aim(key, noise, rng);
        slipped |= hit != key;
        engine.set_next_key_touch(touch);
        apply(&mut app, &engine.process_char(hit), hit);
    }
    apply(&mut app, &engine.process_char(' '), ' ');

    // What a platform does before it looks at candidates: a word that is already a
    // word is what the user meant, however odd it looks to the engine.
    let vetoed = store.knows(app.trim_end());
    let chosen = if vetoed {
        None
    } else {
        decide(
            engine.correction_candidates(),
            store,
            options.margin,
            options.known_only,
            options.max_edits,
        )
    };
    let corrected = chosen
        .and_then(|index| engine.correction_candidates().get(index))
        .map(|candidate| candidate.text().to_owned());
    apply(&mut app, &engine.apply_correction(chosen), ' ');

    match (slipped, corrected) {
        (false, _) => Outcome::Typed,
        (true, Some(word)) if word == syllable => Outcome::Fixed,
        (true, Some(_)) => Outcome::Wrong,
        // Nothing was offered. Either the broken word is still a real syllable — and
        // correction is right to leave it — or it is not and we simply missed it.
        (true, None) if funput_core::is_complete_syllable(app.trim_end()) => Outcome::Homophone,
        (true, None) => Outcome::Missed,
    }
}

fn apply(app: &mut String, result: &funput_engine::ImeResult, key: char) {
    if result.action == Action::None {
        app.push(key);
        return;
    }
    for _ in 0..result.backspace {
        app.pop();
    }
    app.push_str(&result.output);
}

pub(super) struct Options {
    pub(super) method: InputMethod,
    pub(super) prior: Prior,
    pub(super) noise: f32,
    /// How far ahead the winner must be before it is applied. 1.0 is the engine's
    /// own Δ; the harness can vary it to find where the wrong corrections go away.
    pub(super) margin: f32,
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
    let mut rng = Rng::new(options.seed);
    let store = Store::learn(syllables, options.prior);
    let mut tally = Tally::default();
    for syllable in syllables {
        let outcome = attempt(syllable, options, &store, &mut rng);
        tally.add(outcome);
        if tally.samples.len() < options.show && outcome != Outcome::Typed {
            let keys = encode(syllable, options.method);
            tally.samples.push((syllable.clone(), keys, outcome));
        }
    }
    tally
}

#[cfg(test)]
mod tests;
