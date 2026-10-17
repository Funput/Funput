//! The human rendering of a [`Tally`].

use std::path::Path;

use super::{Outcome, Tally, faults};
use crate::dev::typos::Options;

pub(in crate::dev::typos) fn print_human(
    corpus: &Path,
    syllables: &[String],
    tally: &Tally,
    options: &Options,
) {
    println!(
        "Funput typo correction — {} syllables, finger noise {:.2} pitches, {} (corpus: {})",
        syllables.len(),
        options.noise,
        seeds(options),
        corpus.display()
    );
    if options.faults.overall > 0.0 {
        println!(
            "  one typing fault in {:.0}% of words{}",
            options.faults.overall * 100.0,
            options
                .faults
                .only
                .map_or(String::new(), |fault| format!(", always {}", fault.name()))
        );
    }
    // Every seed types the whole corpus once more.
    let words = syllables.len() * options.seeds.max(1) as usize;
    println!("  typed clean   {:>7}", tally.typed);
    println!(
        "  slips         {:>7}  ({:.1}% of words)",
        tally.slips(),
        pct(tally.slips(), words)
    );
    println!(
        "    fixed       {:>7}  ({:.1}% of slips)",
        tally.fixed,
        pct(tally.fixed, tally.slips())
    );
    println!(
        "    wrong       {:>7}  ({:.2}% of corrections applied)",
        tally.wrong,
        pct(tally.wrong, tally.corrections())
    );
    println!(
        "    missed      {:>7}  ({:.1}% of slips: {} no candidate, {} vetoed, {} Δ, {} as typed, {} refused)",
        tally.missed(),
        pct(tally.missed(), tally.slips()),
        tally.missed[0],
        tally.missed[1],
        tally.missed[2],
        tally.missed[3],
        tally.missed[4]
    );
    println!(
        "    another word{:>7}  (a real syllable — correction must not touch it)",
        tally.homophone
    );
    println!(
        "  rewritten     {:>7}  (typed right, changed anyway)",
        tally.rewritten
    );
    faults::print_table(&tally.by_fault);
    if !tally.samples.is_empty() {
        println!("\nSamples: syllable → keys → outcome");
        for (syllable, keys, outcome) in &tally.samples {
            println!("  {syllable} → {keys} → {}", label(*outcome));
        }
    }
}

fn label(outcome: Outcome) -> &'static str {
    match outcome {
        Outcome::Typed => "typed clean",
        Outcome::Fixed => "fixed",
        Outcome::Wrong => "WRONG",
        Outcome::Missed(_) => "missed",
        Outcome::Homophone => "another word",
        Outcome::Rewritten => "REWRITTEN",
    }
}

fn seeds(options: &Options) -> String {
    match options.seeds {
        0 | 1 => format!("seed {}", options.seed),
        n => format!("seeds {}..{}", options.seed, options.seed + n - 1),
    }
}

pub(super) fn pct(part: usize, whole: usize) -> f64 {
    if whole == 0 {
        return 0.0;
    }
    part as f64 * 100.0 / whole as f64
}
