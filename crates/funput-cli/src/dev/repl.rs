//! Interactive line-by-line REPL — dependency-free (no raw terminal mode).
//!
//! Type a line + Enter; the engine processes it as a fresh word stream and the
//! result is printed. Quit with `:q` or Ctrl-D (EOF).

use std::io::{self, BufRead, Write};

use funput_core::InputMethod;

use super::render::steps_table;
use super::sim::simulate;

pub fn run(method: InputMethod, steps: bool) {
    let name = match method {
        InputMethod::Telex => "telex",
        InputMethod::TelexAdvanced => "telex-advanced",
        InputMethod::Vni => "vni",
        _ => "other",
    };
    // Banner on stderr so stdout stays clean for piping.
    eprintln!("funput repl [{name}] — type a line then Enter; :q or Ctrl-D to quit.");

    let stdin = io::stdin();
    let mut stdout = io::stdout();
    for line in stdin.lock().lines() {
        let Ok(line) = line else { break };
        if line == ":q" {
            break;
        }
        let sim = simulate(method, &line);
        let rendered = if steps {
            steps_table(&sim)
        } else {
            sim.app_text
        };
        let _ = writeln!(stdout, "{rendered}");
        let _ = stdout.flush();
    }
}
