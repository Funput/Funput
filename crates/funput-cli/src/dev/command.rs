//! Dispatching `funput dev` to the tool the subcommand names.

use std::path::PathBuf;
use std::process::ExitCode;

use super::render::steps_table;
use super::{DevArgs, DevCommand, coverage, repl, sim, typos};
use crate::cli::{CliError, CliResult};

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
        DevCommand::Typos {
            corpus,
            method,
            noise,
            seed,
            margin,
            known_only,
            max_edits,
            uniform_prior,
            limit,
            show,
            json,
        } => {
            let path = corpus.unwrap_or_else(|| PathBuf::from("benchmarks/sample.txt"));
            let options = typos::Options {
                method: method.into(),
                prior: if uniform_prior {
                    typos::Prior::Uniform
                } else {
                    typos::Prior::Corpus
                },
                noise,
                margin,
                known_only,
                max_edits,
                seed,
                limit,
                show,
                json,
            };
            typos::run(&path, &options).map_err(|e| {
                CliError::Msg(format!("typos: cannot read corpus {}: {e}", path.display()))
            })?;
        }
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
