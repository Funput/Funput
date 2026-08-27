//! `funput` — the umbrella binary for Funput's command-line surface.
//!
//! Two families of subcommands, each a self-contained module owning its args and
//! `run()` handler:
//! - `funput term …` — type Vietnamese inside terminal apps via a PTY wrapper
//!   (the reusable [`funput_term`] library; not an IME, no permissions).
//! - `funput dev …` — feed a string through `funput-engine` and print what a
//!   platform shell would show, for quick checks, debugging, and CI.
//! - `funput convert …` — turn a document between Unicode and the legacy charsets
//!   Vietnamese government paperwork still arrives in (TCVN3, VNI-Windows).

mod cli;
mod convert;
mod dev;
mod term;

use std::process::ExitCode;

use clap::Parser;

use cli::{Cli, Command};

fn main() -> ExitCode {
    let result = match Cli::parse().command {
        Command::Term(args) => term::run(args),
        Command::Dev(args) => dev::run(args),
        Command::Convert(args) => convert::run(args),
    };
    match result {
        Ok(code) => code,
        Err(err) => {
            eprintln!("{err}");
            ExitCode::FAILURE
        }
    }
}
