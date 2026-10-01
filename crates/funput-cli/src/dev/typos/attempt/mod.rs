//! Typing one word with a wandering finger, answering the engine the way a keyboard
//! does, and naming what became of the word.

pub(super) mod keep;
mod word;

use funput_engine::{Action, Engine, ImeResult};

use super::Options;
use super::noise::{Rng, aim};
use super::report::{Miss, Outcome};
use super::store::{Store, ballots};
pub(in crate::dev) use keep::run_keep;
use word::same_word;

/// Type `keys` — meant to produce `word` — with a wandering finger, and report what
/// correction made of it.
///
/// `store` stands in for the platform's word store, and is used exactly the way a
/// keyboard uses it: veto first, then weigh the candidates.
pub(super) fn attempt(
    word: &str,
    keys: &str,
    options: &Options,
    store: &Store,
    rng: &mut Rng,
) -> Outcome {
    let (method, noise) = (options.method, options.noise);
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.typo_correction = true;
    });
    let mut app = String::new();
    let mut slipped = false;
    for key in keys.chars() {
        let (hit, touch) = aim(key, noise, rng);
        slipped |= hit != key;
        engine.set_next_key_touch(touch);
        apply(&mut app, &engine.process_char(hit), hit);
    }
    apply(&mut app, &engine.process_char(' '), ' ');

    let answer = answer(&mut engine, app.trim_end(), options, store);
    let corrected = answer
        .ok()
        .and_then(|index| engine.correction_candidates().get(index))
        .map(|candidate| candidate.text().to_owned());
    apply(&mut app, &engine.apply_correction(answer.ok()), ' ');

    match (slipped, corrected, answer) {
        (false, None, _) => Outcome::Typed,
        (false, Some(_), _) => Outcome::Rewritten,
        (true, Some(fixed), _) if same_word(&fixed, word) => Outcome::Fixed,
        (true, Some(_), _) => Outcome::Wrong,
        // Nothing was offered. Either the broken word is still a real syllable — and
        // correction is right to leave it — or it is not and we simply missed it.
        (true, None, _) if funput_core::is_complete_syllable(app.trim_end()) => Outcome::Homophone,
        (true, None, Err(miss)) => Outcome::Missed(miss),
        (true, None, Ok(_)) => unreachable!("an answer always names a candidate"),
    }
}

/// What the keyboard answers the parked correction with: a candidate, or why not.
fn answer(
    engine: &mut Engine,
    shown: &str,
    options: &Options,
    store: &Store,
) -> Result<usize, Miss> {
    if !engine.has_pending_correction() {
        return Err(Miss::NoCandidate);
    }
    // A word that is already a word is what the user meant, however odd it looks to
    // the engine.
    if store.knows(shown) {
        return Err(Miss::Vetoed);
    }
    let (uses, allowed) = ballots(
        engine.correction_candidates(),
        store,
        options.known_only,
        options.max_edits,
    );
    let before = engine.correction_metrics();
    let chosen = engine.choose_correction(&uses, &allowed);
    let after = engine.correction_metrics();
    chosen.ok_or(if after.kept_as_typed > before.kept_as_typed {
        Miss::AsTyped
    } else if after.skipped_ambiguous > before.skipped_ambiguous {
        Miss::Ambiguous
    } else {
        Miss::Refused
    })
}

fn apply(app: &mut String, result: &ImeResult, key: char) {
    if result.action == Action::None {
        app.push(key);
        return;
    }
    for _ in 0..result.backspace {
        app.pop();
    }
    app.push_str(&result.output);
}
