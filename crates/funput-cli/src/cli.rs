//! Top-level command-line surface (clap derive) for the `funput` umbrella binary.
//!
//! Splits into product families — user-facing [`term`](crate::term) and dev/CI
//! [`dev`](crate::dev) — each owning its own args and `run()` handler. Adding a
//! future product is another [`Command`] variant plus a module with a `run()` entry.

use std::fmt;
use std::process::ExitCode;

use clap::{Parser, Subcommand, ValueEnum};

use funput_core::InputMethod;

use crate::dev::DevArgs;
use crate::term::TermArgs;

#[derive(Debug, Parser)]
#[command(
    name = "funput",
    version,
    about = "Funput — Vietnamese input, from the terminal"
)]
pub struct Cli {
    #[command(subcommand)]
    pub command: Command,
}

#[derive(Debug, Subcommand)]
pub enum Command {
    /// Type Vietnamese (Telex/VNI) inside terminal apps via a PTY wrapper.
    Term(TermArgs),
    /// Developer & CI tools that drive funput-engine directly.
    Dev(DevArgs),
}

/// Input method as selected on the command line. Kept at the CLI layer (a clap
/// `ValueEnum`) so `funput_core` need not depend on clap; shared by every command.
#[derive(Debug, Clone, Copy, ValueEnum)]
pub enum MethodArg {
    Telex,
    Vni,
}

impl From<MethodArg> for InputMethod {
    fn from(m: MethodArg) -> Self {
        match m {
            MethodArg::Telex => InputMethod::Telex,
            MethodArg::Vni => InputMethod::Vni,
        }
    }
}

/// Error surfaced by a command handler. `main` prints it and maps to a failure
/// exit code — the single place the CLI reports errors.
#[derive(Debug)]
pub enum CliError {
    Io(std::io::Error),
    Msg(String),
}

/// A handler's result: the process [`ExitCode`] on success, or a [`CliError`].
pub type CliResult = Result<ExitCode, CliError>;

impl fmt::Display for CliError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            CliError::Io(e) => write!(f, "{e}"),
            CliError::Msg(m) => write!(f, "{m}"),
        }
    }
}

impl std::error::Error for CliError {}

impl From<std::io::Error> for CliError {
    fn from(e: std::io::Error) -> Self {
        CliError::Io(e)
    }
}
