//! `funput-lexicon-tool` — builds the ranked word list behind the English lexicon.
//!
//! Three steps, split so the expensive one runs once:
//! - `count` reads one decompressed Google Books Ngram v3 1-gram file on stdin and
//!   prints `word\tcount` for the years the lexicon ranks by.
//! - `rank` joins those counts with the SCOWL word list, the supplement and the
//!   blocklist, and prints `en.tsv`.
//! - `pack` turns `en.tsv` into the `en.lex` the keyboards map, verified by the
//!   library that will read it.
//! - `doubled` lists the `en.tsv` words a Telex revert shortens, and the prefixes
//!   at which the engine may restore them mid-word, for doubled-key restore.
//!
//! See `README.md` for the whole refresh, and
//! `docs/features/english-lexicon-suggestion.md` for why the data is shaped so.

mod count;
mod doubled;
mod forms;
mod pack;
mod rank;

use std::io::{self, BufWriter};
use std::process::ExitCode;

/// The longest word the lexicon keeps — the suggestion engine's own token limit.
pub(crate) const MAX_LEN: usize = 32;

/// The shortest word worth offering: the shells ask for nothing under two letters.
pub(crate) const MIN_LEN: usize = 2;

const USAGE: &str = "\
usage:
  funput-lexicon-tool count < 1-NNNNN-of-00024        (decompressed)
  funput-lexicon-tool rank --allowed FILE [--supplement FILE] [--blocklist FILE]
                           [--top N] COUNTS...
  funput-lexicon-tool pack < en.tsv > en.lex
  funput-lexicon-tool doubled [--overrides FILE] --out DIR < en.tsv
                              (writes DIR/words.txt and DIR/decisive.txt)";

fn main() -> ExitCode {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let mut out = BufWriter::new(io::stdout().lock());
    let result = match args.first().map(String::as_str) {
        Some("count") => count::run(io::stdin().lock(), &mut out),
        Some("pack") => pack::run(io::stdin().lock(), &mut out),
        Some("doubled") => match doubled::Options::parse(&args[1..]) {
            Ok(options) => doubled::run(&options, io::stdin().lock()),
            Err(message) => return usage(&message),
        },
        Some("rank") => match rank::Options::parse(&args[1..]) {
            Ok(options) => rank::run(&options, &mut out),
            Err(message) => return usage(&message),
        },
        _ => return usage("expected a subcommand"),
    };
    match result {
        Ok(()) => ExitCode::SUCCESS,
        Err(error) => {
            eprintln!("funput-lexicon-tool: {error}");
            ExitCode::FAILURE
        }
    }
}

fn usage(message: &str) -> ExitCode {
    eprintln!("funput-lexicon-tool: {message}\n{USAGE}");
    ExitCode::from(2)
}
