//! Human-readable rendering of a simulation's per-keystroke steps.

use std::fmt::Write as _;

use funput_engine::Action;

use crate::sim::Simulation;

/// Render the per-keystroke table plus the final app text.
pub fn steps_table(sim: &Simulation) -> String {
    let mut out = String::new();
    let _ = writeln!(
        out,
        "{:<3} {:<5} {:<7} {:<3} {:<8} buffer",
        "#", "key", "action", "bs", "output"
    );
    for (i, step) in sim.steps.iter().enumerate() {
        let _ = writeln!(
            out,
            "{:<3} {:<5} {:<7} {:<3} {:<8} {}",
            i + 1,
            show_char(step.key),
            action_name(step.action),
            step.backspace,
            show_str(&step.output),
            show_str(&step.buffer),
        );
    }
    let _ = write!(out, "→ {}", show_str(&sim.app_text));
    out
}

fn action_name(action: Action) -> &'static str {
    match action {
        Action::None => "None",
        Action::Send => "Send",
        Action::Restore => "Restore",
    }
}

fn show_char(c: char) -> String {
    match c {
        ' ' => "␣".to_string(),
        '\t' => "⇥".to_string(),
        '\n' => "⏎".to_string(),
        other => other.to_string(),
    }
}

fn show_str(s: &str) -> String {
    if s.is_empty() {
        "-".to_string()
    } else {
        s.replace(' ', "␣")
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::sim::{simulate, Method};

    #[test]
    fn table_has_header_rows_and_summary() {
        let table = steps_table(&simulate(Method::Telex, "as"));
        let lines: Vec<&str> = table.lines().collect();
        assert!(lines[0].starts_with("#"));
        assert_eq!(lines.len(), 1 + 2 + 1); // header + 2 steps + summary
        assert!(lines.last().unwrap().contains("á"));
    }
}
