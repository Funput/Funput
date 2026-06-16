//! `funput` — terminal dev tool driving `funput-engine` (Telex/VNI).
//!
//! Not a real IME: no keyboard hooks, no injecting into other apps. It feeds an
//! input string through the engine and prints what a platform shell would show,
//! for quick checks, debugging, and CI.

mod cli;
mod render;
mod repl;
mod sim;

use clap::Parser;

use cli::{Cli, Command};
use render::steps_table;

fn main() {
    match Cli::parse().command {
        Command::Run { input, opts } => {
            let simulation = sim::simulate(opts.method.into(), &input);
            if opts.steps {
                println!("{}", steps_table(&simulation));
            } else {
                println!("{}", simulation.app_text);
            }
        }
        Command::Repl { opts } => repl::run(opts.method.into(), opts.steps),
    }
}
