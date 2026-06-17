//! `funput-term` — type Vietnamese inside terminal apps via a transparent PTY wrapper.
//!
//! Run a program through it (`funput-term -- claude`) and ASCII keystrokes are
//! composed into Vietnamese before reaching the child; everything else is
//! forwarded untouched. Toggle with `Ctrl-\`. Not an IME — no system hooks, no
//! permissions; works in any terminal emulator.

mod app;
mod inject;
mod input;
mod output;
mod state;
mod term;

use clap::{Parser, ValueEnum};
use funput_core::InputMethod;

/// `Ctrl-\` — toggles Vietnamese composition on/off.
const TOGGLE_KEY: u8 = 0x1c;

#[derive(Parser)]
#[command(
    name = "funput-term",
    version,
    about = "Type Vietnamese (Telex/VNI) inside terminal apps via a PTY wrapper"
)]
struct Cli {
    /// Input method.
    #[arg(short, long, value_enum, default_value_t = Method::Vni)]
    method: Method,

    /// Program to run (defaults to $SHELL). Pass after `--`, e.g. `funput-term -- claude`.
    #[arg(trailing_var_arg = true, allow_hyphen_values = true)]
    command: Vec<String>,
}

#[derive(Clone, Copy, ValueEnum)]
enum Method {
    Telex,
    Vni,
}

impl From<Method> for InputMethod {
    fn from(m: Method) -> Self {
        match m {
            Method::Telex => InputMethod::Telex,
            Method::Vni => InputMethod::Vni,
        }
    }
}

fn main() {
    let cli = Cli::parse();

    let command = if cli.command.is_empty() {
        vec![std::env::var("SHELL").unwrap_or_else(|_| "/bin/sh".to_string())]
    } else {
        cli.command
    };

    let opts = app::Options {
        method: cli.method.into(),
        toggle: TOGGLE_KEY,
        command,
    };

    match app::run(opts) {
        Ok(code) => std::process::exit(code),
        Err(err) => {
            eprintln!("funput-term: {err}");
            std::process::exit(1);
        }
    }
}
