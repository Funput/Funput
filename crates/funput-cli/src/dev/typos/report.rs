//! Rendering the harness results — a human report and the JSON a gate can read.

use std::path::Path;

use super::Options;
use super::tally::{Outcome, Tally};

pub(super) fn print_human(corpus: &Path, syllables: &[String], tally: &Tally, options: &Options) {
    println!(
        "Funput typo correction — {} syllables, finger noise {:.2} pitches, seed {} (corpus: {})",
        syllables.len(),
        options.noise,
        options.seed,
        corpus.display()
    );
    println!("  typed clean   {:>7}", tally.typed);
    println!(
        "  slips         {:>7}  ({:.1}% of words)",
        tally.slips(),
        pct(tally.slips(), syllables.len())
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
        "    missed      {:>7}  ({:.1}% of slips)",
        tally.missed,
        pct(tally.missed, tally.slips())
    );
    println!(
        "    another word{:>7}  (a real syllable — correction must not touch it)",
        tally.homophone
    );
    if !tally.samples.is_empty() {
        println!("\nSamples: syllable → keys → outcome");
        for (syllable, keys, outcome) in &tally.samples {
            println!("  {syllable} → {keys} → {}", label(*outcome));
        }
    }
}

pub(super) fn print_json(corpus: &Path, syllables: &[String], tally: &Tally, options: &Options) {
    println!("{{");
    println!("  \"corpus\": \"{}\",", corpus.display());
    println!("  \"syllables\": {},", syllables.len());
    println!("  \"noise\": {:.2},", options.noise);
    println!("  \"seed\": {},", options.seed);
    println!("  \"typed\": {},", tally.typed);
    println!("  \"slips\": {},", tally.slips());
    println!("  \"fixed\": {},", tally.fixed);
    println!("  \"wrong\": {},", tally.wrong);
    println!("  \"missed\": {},", tally.missed);
    println!("  \"another_word\": {},", tally.homophone);
    println!(
        "  \"fixed_pct_of_slips\": {:.2},",
        pct(tally.fixed, tally.slips())
    );
    println!(
        "  \"wrong_pct_of_corrections\": {:.2}",
        pct(tally.wrong, tally.corrections())
    );
    println!("}}");
}

fn label(outcome: Outcome) -> &'static str {
    match outcome {
        Outcome::Typed => "typed clean",
        Outcome::Fixed => "fixed",
        Outcome::Wrong => "WRONG",
        Outcome::Missed => "missed",
        Outcome::Homophone => "another word",
    }
}

fn pct(part: usize, whole: usize) -> f64 {
    if whole == 0 {
        return 0.0;
    }
    part as f64 * 100.0 / whole as f64
}
