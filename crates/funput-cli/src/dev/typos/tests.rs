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

/// Measured at the spread read off a device (0.174 pitches), summed over seeds
/// because this corpus is small and a real finger rarely misses.
///
/// This gate used to sit at 0.25, the design's guess before anyone had measured a
/// finger. Letting the typed word compete (so a deliberate `ko` survives) costs
/// about five points of repairs out there and next to nothing at the real spread,
/// and the real spread is what users type with.
#[test]
fn at_realistic_noise_it_repairs_most_of_what_it_can() {
    let syllables: Vec<String> = load_syllables(&sample_path())
        .expect("load sample corpus")
        .into_iter()
        .collect();
    let (mut fixed, mut slips) = (0, 0);
    for seed in 1..=10 {
        let tally = measure(
            &syllables,
            &Options {
                seed,
                ..options(0.174, Prior::Corpus)
            },
        );
        fixed += tally.fixed;
        slips += tally.slips();
    }
    assert!(slips > 20, "the run must actually exercise slips");
    let rate = fixed as f64 * 100.0 / slips as f64;
    assert!(
        rate >= 50.0,
        "only {rate:.1}% of {slips} slips were repaired"
    );
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

fn keep_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../funput-suggestions/data/correction/keep.txt")
}

/// The engine's own guard for words typed on purpose. Chat abbreviations and brand
/// names are not Vietnamese, and several sit one key from a syllable; before the
/// typed word was allowed to compete, a quarter of them came out rewritten at a real
/// finger's spread. The engine cannot get this to zero without refusing real slips
/// that land deep in the wrong key (`abh` for `anh`) — a touch near a key's edge
/// looks exactly like a slip — so it keeps it well under that quarter, and the
/// keyboards veto the listed words by name.
#[test]
fn a_word_typed_on_purpose_is_rarely_rewritten() {
    let words = attempt::keep::load_words(&keep_path()).expect("load keep list");
    for method in [InputMethod::Telex, InputMethod::Vni] {
        let (mut typed, mut rewritten) = (0, 0);
        for seed in 1..=20 {
            let tally = attempt::keep::measure_keep(
                &words,
                &Options {
                    method,
                    seed,
                    ..options(0.174, Prior::Uniform)
                },
            );
            typed += tally.typed + tally.rewritten;
            rewritten += tally.rewritten;
        }
        assert!(
            rewritten * 10 < typed,
            "{method:?}: {rewritten} of {typed} deliberate words were rewritten"
        );
    }
}

/// A slip is not a reason to rewrite a word the user did not mistype: across a clean
/// corpus, every correction applied is to a word where a key really missed.
#[test]
fn a_syllable_typed_right_is_never_rewritten() {
    let (_, tally) = run(0.25);
    assert_eq!(tally.rewritten, 0);
}

/// Every broken word that stays broken is counted under exactly one reason.
#[test]
fn every_miss_has_a_reason() {
    let (_, tally) = run_with(0.3, Prior::Uniform);
    assert_eq!(tally.missed(), tally.missed.iter().sum::<usize>());
    assert!(tally.missed() > 0, "a noisy run must miss something");
}
