//! `funput dev`: drive funput-engine from the terminal for quick checks, debugging,
//! and CI. Not a real IME — no keyboard hooks, no injecting into other apps. Owns
//! its clap surface, its handler, and the engine-simulation tooling below it.

mod command;
mod coverage;
mod encode;
mod render;
mod repl;
mod sim;
mod typos;

use std::path::PathBuf;

use clap::{Args, Subcommand};

use crate::cli::MethodArg;

pub use command::run;

#[derive(Debug, Args)]
pub struct DevArgs {
    #[command(subcommand)]
    pub command: DevCommand,
}

#[derive(Debug, Subcommand)]
pub enum DevCommand {
    /// Transform an input string and print the resulting app text.
    Run {
        /// Keys to type. A literal string — spaces and punctuation are word boundaries.
        input: String,
        #[command(flatten)]
        opts: CommonOpts,
    },
    /// Interactive REPL: type a line, see the result, repeat (Ctrl-D or `:q` to quit).
    Repl {
        #[command(flatten)]
        opts: CommonOpts,
    },
    /// Measure typo correction: type a corpus with a wandering finger and count
    /// what correction fixes, misses, and — the number that matters — gets wrong.
    Typos {
        /// Corpus file (one word per line). Defaults to `benchmarks/sample.txt`.
        corpus: Option<PathBuf>,
        /// Input method to encode and type with.
        #[arg(short, long, value_enum, default_value_t = MethodArg::Telex)]
        method: MethodArg,
        /// How far the finger really lands from a key centre, in key pitches. Not
        /// the engine's scoring σ — that is its belief about the same spread, and
        /// the interesting runs are the ones where the two differ.
        #[arg(long, default_value_t = 0.25)]
        noise: f32,
        /// Seed for the touch noise, so a run is reproducible.
        #[arg(long, default_value_t = 1)]
        seed: u64,
        /// Only offer candidates the word store recognizes.
        #[arg(long)]
        known_only: bool,
        /// How many substituted keys a candidate may carry.
        #[arg(long, default_value_t = 2)]
        max_edits: usize,
        /// Which word store to rank with: `shipped` is the list Funput bundles and
        /// is what a device sees, `corpus` learns the corpus itself and is the
        /// optimistic bound, `uniform` is a host that wired no store at all.
        #[arg(long, default_value = "shipped")]
        prior: String,
        /// Cap the number of syllables evaluated (for a quick run).
        #[arg(long)]
        limit: Option<usize>,
        /// Print up to N sample words that were not typed cleanly.
        #[arg(long, default_value_t = 0)]
        show: usize,
        /// Emit machine-readable JSON instead of a human report.
        #[arg(long)]
        json: bool,
    },
    /// Round-trip coverage check over a Vietnamese corpus (Telex, Advanced & VNI).
    Coverage {
        /// Corpus file (one word per line). Defaults to `benchmarks/sample.txt`.
        corpus: Option<PathBuf>,
        /// Emit machine-readable JSON instead of a human report.
        #[arg(long)]
        json: bool,
        /// Print up to N sample mismatches per method.
        #[arg(long, default_value_t = 0)]
        show_mismatches: usize,
        /// Cap the number of syllables evaluated (for a quick run).
        #[arg(long)]
        limit: Option<usize>,
    },
}

#[derive(Debug, Args)]
pub struct CommonOpts {
    /// Input method.
    #[arg(short, long, value_enum, default_value_t = MethodArg::Vni)]
    pub method: MethodArg,
    /// Print per-keystroke detail instead of just the final app text.
    #[arg(long)]
    pub steps: bool,
}
