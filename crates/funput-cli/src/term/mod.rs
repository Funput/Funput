//! `funput term`: run a program through the PTY wrapper, or manage shell
//! integration. Owns its clap surface and its handler.

use std::process::ExitCode;

use clap::{Args, Subcommand};

use crate::cli::{CliError, CliResult, MethodArg};

/// `funput term`: run a program through the wrapper, or manage shell integration.
///
/// Mirrors the old standalone `funput-term` CLI: a default run action plus an
/// `install` subcommand. `args_conflicts_with_subcommands` keeps `funput term
/// install …` from being parsed as a program to run.
#[derive(Debug, Args)]
#[command(args_conflicts_with_subcommands = true)]
pub struct TermArgs {
    #[command(subcommand)]
    pub command: Option<TermCommand>,

    #[command(flatten)]
    pub run: TermRunArgs,
}

/// Arguments for the default action: run a program through the wrapper.
#[derive(Debug, Args)]
pub struct TermRunArgs {
    /// Input method; overrides the config file and `$FUNPUT_METHOD`.
    #[arg(short, long, value_enum)]
    pub method: Option<MethodArg>,

    /// Program to run (defaults to `$SHELL`). Pass after `--`, e.g. `funput term -- claude`.
    #[arg(trailing_var_arg = true, allow_hyphen_values = true)]
    pub program: Vec<String>,
}

#[derive(Debug, Subcommand)]
pub enum TermCommand {
    /// Print (or write) shell integration so Funput Terminal is always on.
    Install {
        /// Target shell (bash/zsh/fish); defaults to `$SHELL`.
        #[arg(long)]
        shell: Option<String>,

        /// Alias to add, `name` or `name=command`; repeatable.
        #[arg(long = "alias", value_name = "NAME[=CMD]")]
        alias: Vec<String>,

        /// Append the snippet to your shell rc file instead of just printing it.
        #[arg(long)]
        write: bool,
    },
}

/// Run `funput term`: dispatch to `install`, or the default wrapper action.
pub fn run(args: TermArgs) -> CliResult {
    match args.command {
        Some(TermCommand::Install {
            shell,
            alias,
            write,
        }) => install(shell, alias, write),
        None => wrapper(args.run),
    }
}

fn install(shell: Option<String>, alias: Vec<String>, write: bool) -> CliResult {
    let shell = shell
        .as_deref()
        .and_then(funput_term::install::Shell::from_name)
        .unwrap_or_else(funput_term::install::Shell::detect);
    let aliases: Vec<_> = alias
        .iter()
        .map(|a| funput_term::install::parse_alias(a))
        .collect();
    funput_term::install::run(shell, &aliases, write)
        .map_err(|e| CliError::Msg(format!("funput term: {e}")))?;
    Ok(ExitCode::SUCCESS)
}

/// Default action: run `program` (or `$SHELL`) through the wrapper. Precedence for
/// the input method: CLI flag > env var > settings.json > default.
fn wrapper(run: TermRunArgs) -> CliResult {
    let mut config = funput_term::config::load();
    if let Some(method) = run.method {
        config.method = method.into();
    }

    let command = if run.program.is_empty() {
        vec![std::env::var("SHELL").unwrap_or_else(|_| "/bin/sh".to_string())]
    } else {
        run.program
    };

    let code = funput_term::runtime::run(funput_term::runtime::Options { config, command })
        .map_err(|e| CliError::Msg(format!("funput term: {e}")))?;
    Ok(ExitCode::from(code as u8))
}
