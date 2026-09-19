//! The gate: what typo correction may and may not do to a corpus.

use std::path::PathBuf;

use funput_core::InputMethod;

use super::*;

fn sample_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../benchmarks/sample.txt")
}

fn options(noise: f32) -> Options {
    Options {
        method: InputMethod::Telex,
        noise,
        seed: 1,
        limit: None,
        show: 0,
        json: false,
    }
}

fn run(noise: f32) -> (Vec<String>, Tally) {
    let syllables: Vec<String> = load_syllables(&sample_path())
        .expect("load sample corpus")
        .into_iter()
        .collect();
    let tally = measure(&syllables, &options(noise));
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

/// Above this the model's assumptions break down: nearly every word carries more
/// than one slip, two substitutions reach words nobody typed, and touch evidence
/// alone stops telling them apart. The platform's word frequencies are what buy the
/// margin back, and the engine has none here — so this records where the floor is
/// rather than pretending it is safe.
#[test]
fn a_very_noisy_finger_outruns_touch_evidence_alone() {
    let (_, tally) = run(0.45);
    assert!(
        tally.wrong * 4 > tally.corrections(),
        "σ 0.45 used to put a quarter of corrections wrong; if that has improved, \
         move the gate and say so ({} of {})",
        tally.wrong,
        tally.corrections()
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
