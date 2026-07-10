//! `funput dev`: drive funput-engine from the terminal for quick checks, debugging,
//! and CI. Not a real IME — no keyboard hooks, no injecting into other apps. Owns
//! its clap surface, its handler, and the engine-simulation tooling below it.

mod coverage;
mod encode;
mod render;
mod repl;
mod sim;

use std::path::PathBuf;
use std::process::ExitCode;

use clap::{Args, Subcommand};

use crate::cli::{CliError, CliResult, MethodArg};
use render::steps_table;

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
    /// Round-trip coverage check over a Vietnamese corpus (Telex & VNI).
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

/// Run `funput dev`: dispatch to the selected engine tool.
pub fn run(args: DevArgs) -> CliResult {
    match args.command {
        DevCommand::Run { input, opts } => {
            let simulation = sim::simulate(opts.method.into(), &input);
            if opts.steps {
                println!("{}", steps_table(&simulation));
            } else {
                println!("{}", simulation.app_text);
            }
        }
        DevCommand::Repl { opts } => repl::run(opts.method.into(), opts.steps),
        DevCommand::Coverage {
            corpus,
            json,
            show_mismatches,
            limit,
        } => {
            let path = corpus.unwrap_or_else(|| PathBuf::from("benchmarks/sample.txt"));
            coverage::run(&path, json, show_mismatches, limit).map_err(|e| {
                CliError::Msg(format!(
                    "coverage: cannot read corpus {}: {e}",
                    path.display()
                ))
            })?;
        }
    }
    Ok(ExitCode::SUCCESS)
}
