//! What the harness counts, and how it shows the count.\n//!\n//! The one number that decides the feature is `wrong`: a correction that rewrites a\n//! word into something the user never typed is worse than leaving a broken one alone.

use std::path::Path;

use super::Options;

/// What became of one word.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev) enum Outcome {
    /// Every key landed where it was aimed, and nothing was offered.
    Typed,
    /// A key missed, and correction put the word back.
    Fixed,
    /// A key missed, and correction replaced it with a *different* word. The one
    /// outcome that is worse than doing nothing.
    Wrong,
    /// A key missed and the word stayed broken: nothing was offered, or the top two
    /// were too close to call.
    Missed,
    /// A key missed and landed on another real syllable. Correction must not touch
    /// this — a valid word is what the user meant, as far as the engine can tell.
    Homophone,
}

#[derive(Default)]
pub(super) struct Tally {
    pub(super) typed: usize,
    pub(super) fixed: usize,
    pub(super) wrong: usize,
    pub(super) missed: usize,
    pub(super) homophone: usize,
    pub(super) samples: Vec<(String, String, Outcome)>,
}

impl Tally {
    pub(super) fn add(&mut self, outcome: Outcome) {
        match outcome {
            Outcome::Typed => self.typed += 1,
            Outcome::Fixed => self.fixed += 1,
            Outcome::Wrong => self.wrong += 1,
            Outcome::Missed => self.missed += 1,
            Outcome::Homophone => self.homophone += 1,
        }
    }

    /// Words where a key actually missed — everything the feature could act on.
    pub(super) fn slips(&self) -> usize {
        self.fixed + self.wrong + self.missed + self.homophone
    }

    pub(super) fn corrections(&self) -> usize {
        self.fixed + self.wrong
    }
}

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
