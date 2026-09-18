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
mod tally;

use std::path::Path;

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use super::coverage::corpus::load_syllables;
use super::encode::encode;
use noise::{Rng, aim};
use tally::Outcome;
use tally::Tally;

/// Type `syllable` with a wandering finger and report what correction made of it.
fn attempt(syllable: &str, method: InputMethod, noise: f32, rng: &mut Rng) -> Outcome {
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

    let chosen = engine.choose_correction(&[]);
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
    pub(super) noise: f32,
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
    let mut tally = Tally::default();
    for syllable in syllables {
        let outcome = attempt(syllable, options.method, options.noise, &mut rng);
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
