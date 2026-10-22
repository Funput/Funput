//! English coverage: type every lowercase word of the English lexicon (`en.tsv`)
//! through the engine, then a space, and sort what comes out wrong.
//!
//! Telex cannot know a word is English until it ends, so some words come out wrong
//! by design (`is` → `í`). What must never grow is the count of each kind of wrong:
//! a doubled tone key that swallowed a key (`password` → `pasword`), a word left as
//! Vietnamese, or anything else. `benchmarks/results/english-coverage.txt` holds the
//! counts, and a test fails the moment any of them moves.
//! Design: `docs/features/doubled-key-restore.md` §11.3.

mod baseline;
mod counts;
mod outcome;
mod report;

use std::fs;
use std::path::Path;

use counts::{Counts, evaluate_all};

/// The lexicon `--english` reads unless told otherwise, relative to `app/`.
pub(in crate::dev) const LEXICON: &str = "crates/funput-suggestions/data/lexicon/en.tsv";

/// One configuration the lexicon is typed under.
#[derive(Debug, Clone, Copy)]
pub(super) struct Setup {
    /// Prefix of every count it reports, as the baseline names them.
    pub(super) label: &'static str,
    pub(super) eager_restore: bool,
}

/// Telex with eager restore on (the default) and off: off, a doubled key lands on a
/// composed tone instead of raw letters, so far more words revert.
pub(super) const SETUPS: [Setup; 2] = [
    Setup {
        label: "telex.eager",
        eager_restore: true,
    },
    Setup {
        label: "telex.lazy",
        eager_restore: false,
    },
];

/// What `run` was asked to do besides reporting.
pub(in crate::dev) struct Options<'a> {
    pub(in crate::dev) json: bool,
    pub(in crate::dev) show: usize,
    pub(in crate::dev) limit: Option<usize>,
    /// Compare with this baseline, or overwrite it when `write_baseline` is set.
    pub(in crate::dev) baseline: Option<&'a Path>,
    pub(in crate::dev) write_baseline: bool,
}

/// Type the lexicon at `path`, report, and check or write the baseline. Returns
/// whether every count matches the baseline (always true with no baseline).
pub(in crate::dev) fn run(path: &Path, options: &Options) -> std::io::Result<bool> {
    let tsv = fs::read_to_string(path)?;
    let results = evaluate_all(&tsv, options.limit, options.show);
    if options.json {
        report::print_json(path, &results);
    } else {
        report::print_human(path, &results);
    }
    let Some(file) = options.baseline else {
        return Ok(true);
    };
    let current: Vec<_> = results.iter().flat_map(Counts::entries).collect();
    if options.write_baseline {
        fs::write(file, baseline::format(&current))?;
        eprintln!("wrote {}", file.display());
        return Ok(true);
    }
    let problems = baseline::compare(&baseline::parse(&fs::read_to_string(file)?), &current);
    for problem in &problems {
        eprintln!("{problem}");
    }
    Ok(problems.is_empty())
}

#[cfg(test)]
mod tests;
