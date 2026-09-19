//! The gate: what typo correction may and may not do to a corpus.

use std::path::PathBuf;

use funput_core::InputMethod;

use super::*;

fn sample_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../benchmarks/sample.txt")
}

fn options(noise: f32, prior: Prior) -> Options {
    Options {
        method: InputMethod::Telex,
        prior,
        noise,
        known_only: false,
        max_edits: 2,
        seed: 1,
        limit: None,
        show: 0,
        json: false,
    }
}

fn run(noise: f32) -> (Vec<String>, Tally) {
    run_with(noise, Prior::Corpus)
}

fn run_with(noise: f32, prior: Prior) -> (Vec<String>, Tally) {
    let syllables: Vec<String> = load_syllables(&sample_path())
        .expect("load sample corpus")
        .into_iter()
        .collect();
    let tally = measure(&syllables, &options(noise, prior));
    (syllables, tally)
}

#[test]
fn a_finger_that_does_not_miss_is_never_corrected() {
    let (syllables, tally) = run(0.05);
    assert_eq!(tally.typed, syllables.len());
    assert_eq!(tally.corrections(), 0);
}

/// The number that decides the feature. A wrong correction rewrites a word the user
/// meant, which is worse than leaving a mistyped one alone — so it is capped far
/// below the fix rate it buys.
#[test]
fn at_realistic_noise_a_correction_is_never_wrong() {
    let (_, tally) = run(0.25);
    assert!(tally.slips() > 20, "the run must actually exercise slips");
    let wrong = tally.wrong as f64 * 100.0 / tally.corrections() as f64;
    assert!(
        wrong < 1.0,
        "{wrong:.1}% of corrections were wrong ({} of {})",
        tally.wrong,
        tally.corrections()
    );
}

#[test]
fn at_realistic_noise_it_repairs_most_of_what_it_can() {
    let (_, tally) = run(0.25);
    let fixed = tally.fixed as f64 * 100.0 / tally.slips() as f64;
    assert!(fixed >= 50.0, "only {fixed:.1}% of slips were repaired");
}

/// Without a word store, touch evidence alone stops telling candidates apart once
/// the finger wanders far enough. This records where that floor is rather than
/// pretending it is safe — it is the reason a platform must wire its word store
/// before switching the feature on.
#[test]
fn a_very_noisy_finger_outruns_touch_evidence_alone() {
    let (_, tally) = run_with(0.45, Prior::Uniform);
    assert!(
        tally.wrong * 4 > tally.corrections(),
        "with no word store, noise 0.45 used to put a quarter of corrections wrong; \
         if that has improved, move the gate and say so ({} of {})",
        tally.wrong,
        tally.corrections()
    );
}

/// What the word store is worth, in the only terms that matter.
#[test]
fn a_word_store_is_what_makes_a_noisy_finger_survivable() {
    let (_, without) = run_with(0.45, Prior::Uniform);
    let (_, with) = run_with(0.45, Prior::Corpus);
    assert!(
        with.wrong < without.wrong,
        "the store must cut wrong corrections ({} with, {} without)",
        with.wrong,
        without.wrong
    );
}

#[test]
fn a_run_is_reproducible_from_its_seed() {
    let (_, first) = run(0.3);
    let (_, second) = run(0.3);
    assert_eq!(
        (first.fixed, first.wrong, first.missed),
        (second.fixed, second.wrong, second.missed)
    );
}

#[test]
fn a_wider_spread_makes_more_words_slip() {
    let steps: Vec<usize> = [0.1, 0.2, 0.3, 0.45]
        .iter()
        .map(|&n| run(n).1.slips())
        .collect();
    assert!(
        steps.windows(2).all(|pair| pair[0] <= pair[1]),
        "slips should grow with the spread: {steps:?}"
    );
}
