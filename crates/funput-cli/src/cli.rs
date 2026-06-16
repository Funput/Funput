//! Command-line surface (clap derive).

use clap::{Args, Parser, Subcommand, ValueEnum};

use crate::sim::Method;

#[derive(Debug, Parser)]
#[command(name = "funput", version, about = "Drive funput-engine (Telex/VNI) from the terminal")]
pub struct Cli {
    #[command(subcommand)]
    pub command: Command,
}

#[derive(Debug, Subcommand)]
pub enum Command {
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

#[derive(Debug, Clone, Copy, ValueEnum)]
pub enum MethodArg {
    Telex,
    Vni,
}

impl From<MethodArg> for Method {
    fn from(m: MethodArg) -> Self {
        match m {
            MethodArg::Telex => Method::Telex,
            MethodArg::Vni => Method::Vni,
        }
    }
}
