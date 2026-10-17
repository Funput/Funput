//! The gate: what typo correction may and may not do to a corpus.

use std::path::PathBuf;

use funput_core::InputMethod;

use super::report::Cell;
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
        seeds: 1,
        faults: Rates {
            overall: 0.0,
            only: None,
        },
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

/// With the keyboard's veto in play — `is_known_word`, which carries the keep list —
/// not one listed word typed right is rewritten, however near an edge the finger
/// lands. (A listed word that *slipped* — `kp` for `ko` — is no longer the listed
/// word, and what correction makes of it is a different question.)
#[test]
fn a_listed_word_is_never_rewritten_by_a_keyboard() {
    let words = attempt::keep::load_words(&keep_path()).expect("load keep list");
    for method in [InputMethod::Telex, InputMethod::Vni] {
        for seed in 1..=20 {
            let tally = attempt::keep::measure_keep(
                &words,
                &Options {
                    method,
                    seed,
                    ..options(0.174, Prior::Shipped)
                },
            );
            assert_eq!(tally.rewritten, 0, "{method:?} seed {seed}");
        }
    }
}

/// The shipped bands are a prior, and this is what a prior is for: `nhad` with the
/// finger exactly between `f` and `s` is `nhà` or `nhá` on touch evidence alone, so a
/// host with no store declines. One of the two is among the commonest words in the
/// language and the other is not on the list, and that is enough to decide.
#[test]
fn the_shipped_prior_breaks_a_tie_towards_the_commoner_word() {
    use funput_engine::{Engine, KeyTouch};

    let typed = |prior: Prior| {
        let mut engine = Engine::new();
        engine.update_config(|config| {
            config.method = InputMethod::Telex;
            config.typo_correction = true;
        });
        for key in ['n', 'h', 'a'] {
            engine.set_next_key_touch(KeyTouch::new(key, 0.0));
            engine.process_char(key);
        }
        engine.set_next_key_touch(
            KeyTouch::new('d', 0.3)
                .with_alternate('f', 0.3)
                .with_alternate('s', 0.3),
        );
        engine.process_char('d');
        engine.process_char(' ');
        assert!(engine.has_pending_correction());
        let store = Store::learn(&[], prior);
        let (uses, allowed) = store::ballots(engine.correction_candidates(), &store, false, 2);
        let chosen = engine.choose_correction(&uses, &allowed);
        chosen.map(|index| engine.correction_candidates()[index].text().to_owned())
    };

    assert_eq!(
        typed(Prior::Uniform),
        None,
        "a tie on touch alone is declined"
    );
    assert_eq!(typed(Prior::Shipped).as_deref(), Some("nhà"));
}

/// `--known-only` under the shipped prior admits what the list holds, and nothing a
/// store that has learned nothing could vouch for.
#[test]
fn known_only_under_the_shipped_prior_reads_the_list() {
    let store = Store::learn(&[], Prior::Shipped);
    assert_eq!(
        store.uses("là"),
        funput_suggestions::SYLLABLE_TIER_WEIGHTS[0]
    );
    assert_eq!(store.uses("dduwowfnh"), 0);
    assert!(store.knows("ko"), "the keep list is still the veto");
    assert!(!store.knows("là"), "the list is a prior, not the veto");
}

/// The faults a fast typist makes, and the harness's bookkeeping of them.
mod faults {
    use super::*;
    use crate::dev::typos::inject::{Fault, Rates, inject, modifier_keys};
    use crate::dev::typos::noise::Rng;

    fn always(only: Fault) -> Rates {
        Rates {
            overall: 1.0,
            only: Some(only),
        }
    }

    /// Every word of the sample corpus as keys, in both methods.
    fn corpus_keys() -> Vec<(InputMethod, String)> {
        let syllables = load_syllables(&sample_path()).expect("load sample corpus");
        [InputMethod::Telex, InputMethod::Vni]
            .into_iter()
            .flat_map(|method| {
                syllables
                    .iter()
                    .map(move |syllable| (method, encode::encode(syllable, method)))
            })
            .collect()
    }

    /// Each word faulted with `fault` under a handful of seeds: the original, the
    /// faulty keys, and the method.
    fn faulted(fault: Fault) -> Vec<(InputMethod, Vec<char>, Vec<char>)> {
        let mut out = Vec::new();
        for seed in 1..=5 {
            let mut rng = Rng::new(seed).derived(0xFA17);
            for (method, keys) in corpus_keys() {
                let (faulty, got) = inject(&keys, method, &always(fault), &mut rng);
                if got.is_some() {
                    out.push((method, keys.chars().collect(), faulty.chars().collect()));
                } else {
                    assert_eq!(faulty, keys, "a fault that did not happen changes nothing");
                }
            }
        }
        assert!(out.len() > 200, "the corpus must actually take faults");
        out
    }

    /// Where `longer` first differs from `shorter`, or the end — the one index
    /// whose removal is expected to give `shorter` back.
    fn inserted_at(shorter: &[char], longer: &[char]) -> usize {
        (0..shorter.len())
            .find(|&i| shorter[i] != longer[i])
            .unwrap_or(shorter.len())
    }

    #[test]
    fn injection_is_deterministic_per_seed() {
        let rates = Rates {
            overall: 0.5,
            only: None,
        };
        let run = |seed: u64| -> Vec<(String, Option<Fault>)> {
            let mut rng = Rng::new(seed).derived(0xFA17);
            corpus_keys()
                .iter()
                .map(|(method, keys)| inject(keys, *method, &rates, &mut rng))
                .collect()
        };
        assert_eq!(run(7), run(7));
        assert_ne!(run(7), run(8), "another seed faults other words");
        let faulted = run(7).iter().filter(|(_, fault)| fault.is_some()).count();
        let total = corpus_keys().len();
        assert!(
            (total / 3..=total * 2 / 3).contains(&faulted),
            "{faulted} of {total} words faulted at rate 0.5"
        );
    }

    #[test]
    fn no_rate_means_no_fault() {
        let mut rng = Rng::new(1).derived(0xFA17);
        let rates = Rates {
            overall: 0.0,
            only: None,
        };
        for (method, keys) in corpus_keys() {
            assert_eq!(
                inject(&keys, method, &rates, &mut rng),
                (keys.clone(), None)
            );
        }
    }

    #[test]
    fn a_duplicate_presses_one_key_twice() {
        for (_, keys, faulty) in faulted(Fault::Duplicate) {
            assert_eq!(faulty.len(), keys.len() + 1, "{keys:?} → {faulty:?}");
            let at = inserted_at(&keys, &faulty);
            let mut back = faulty.clone();
            let extra = back.remove(at);
            assert_eq!(back, keys, "{keys:?} → {faulty:?}");
            let twin = at.checked_sub(1).map(|i| faulty[i]) == Some(extra)
                || faulty.get(at + 1) == Some(&extra);
            assert!(
                twin,
                "{keys:?} → {faulty:?}: the extra key must repeat a neighbour"
            );
        }
    }

    #[test]
    fn a_drop_loses_exactly_one_key() {
        for (_, keys, faulty) in faulted(Fault::Drop) {
            assert!(keys.len() >= 2, "a one-key word cannot lose its key");
            assert_eq!(faulty.len(), keys.len() - 1, "{keys:?} → {faulty:?}");
            let at = inserted_at(&faulty, &keys);
            let mut back = keys.clone();
            back.remove(at);
            assert_eq!(back, faulty, "{keys:?} → {faulty:?}");
        }
    }

    #[test]
    fn a_transpose_swaps_two_neighbouring_keys() {
        for (_, keys, faulty) in faulted(Fault::Transpose) {
            assert_eq!(faulty.len(), keys.len(), "{keys:?} → {faulty:?}");
            let differ: Vec<usize> = (0..keys.len()).filter(|&i| keys[i] != faulty[i]).collect();
            assert_eq!(differ.len(), 2, "{keys:?} → {faulty:?}");
            let (i, j) = (differ[0], differ[1]);
            assert_eq!(j, i + 1, "{keys:?} → {faulty:?}: not neighbours");
            assert_eq!((faulty[i], faulty[j]), (keys[j], keys[i]));
        }
    }

    #[test]
    fn an_extra_modifier_is_one_of_the_methods_own() {
        for (method, keys, faulty) in faulted(Fault::ExtraModifier) {
            assert_eq!(faulty.len(), keys.len() + 1, "{keys:?} → {faulty:?}");
            let at = inserted_at(&keys, &faulty);
            let mut back = faulty.clone();
            let extra = back.remove(at);
            assert_eq!(back, keys, "{keys:?} → {faulty:?}");
            assert!(at >= 1, "{keys:?} → {faulty:?}: never before the first key");
            assert!(
                modifier_keys(method).contains(&extra),
                "{keys:?} → {faulty:?}: {extra:?} is no {method:?} modifier"
            );
        }
    }

    #[test]
    fn modifier_keys_are_read_off_key_role() {
        assert_eq!(
            modifier_keys(InputMethod::Telex),
            vec!['f', 'j', 'r', 's', 'w', 'x']
        );
        assert_eq!(
            modifier_keys(InputMethod::Vni),
            ('1'..='8').collect::<Vec<_>>()
        );
    }

    /// The numbers in the docs were measured before faults existed, from `--seed`
    /// alone. A fault draws from a stream of its own, so with faults off a seed
    /// types the very same touches it always did — pinned here, so a future draw
    /// slipped into the touch stream shows up as a failure and not as a mystery
    /// shift in the next calibration.
    #[test]
    fn old_seeds_reproduce_the_touch_stream() {
        let (_, tally) = run(0.25);
        assert_eq!(
            (
                tally.typed,
                tally.fixed,
                tally.wrong,
                tally.missed,
                tally.homophone
            ),
            // E2: one slip that used to be applied now has a structural reading
            // inside the margin and is offered instead (22 fixed, 2 ambiguous before).
            (98, 21, 0, [8, 0, 3, 0, 0], 7)
        );
        let (_, tally) = run_with(0.3, Prior::Uniform);
        assert_eq!(
            (
                tally.typed,
                tally.fixed,
                tally.wrong,
                tally.missed,
                tally.homophone
            ),
            // E2, with no store at all: the structural readings have nothing but
            // touch to lose to, so half of the old repairs are offered instead
            // (30 fixed, 13 ambiguous before). The store is what separates them.
            (65, 15, 0, [12, 0, 29, 5, 0], 11)
        );
    }

    fn with_faults(seed: u64, seeds: u64, rates: Rates) -> Tally {
        let syllables: Vec<String> = load_syllables(&sample_path())
            .expect("load sample corpus")
            .into_iter()
            .collect();
        measure(
            &syllables,
            &Options {
                seed,
                seeds,
                faults: rates,
                ..options(0.25, Prior::Shipped)
            },
        )
    }

    fn counts(tally: &Tally) -> (usize, usize, usize, [usize; 5], usize, usize, [Cell; 5]) {
        (
            tally.typed,
            tally.fixed,
            tally.wrong,
            tally.missed,
            tally.homophone,
            tally.rewritten,
            tally.by_fault,
        )
    }

    #[test]
    fn several_seeds_are_the_sum_of_the_single_runs() {
        let rates = Rates {
            overall: 0.3,
            only: None,
        };
        let mut summed = Tally::default();
        for seed in 1..=3 {
            summed.merge(with_faults(seed, 1, rates));
        }
        assert_eq!(counts(&with_faults(1, 3, rates)), counts(&summed));
        assert!(summed.slips() > 100, "three seeds must exercise slips");
    }

    /// A faulted word is a word not typed as meant, whatever the finger did, and
    /// lands in its own row of the table — never in the touch-slip one.
    #[test]
    fn every_fault_is_counted_in_its_own_row() {
        let tally = with_faults(1, 1, always(Fault::Duplicate));
        assert_eq!(tally.typed, 0, "every word took a fault");
        assert_eq!(tally.by_fault[0].n(), 0);
        assert_eq!(tally.by_fault[1].n(), tally.slips());
        let rows: usize = tally.by_fault.iter().map(Cell::n).sum();
        assert_eq!(rows, tally.slips());
    }
}
