//! Rendering shared by the dev tools: a simulation's per-keystroke steps, and the
//! small number and JSON helpers every report needs.

use std::fmt::Write as _;

use funput_engine::Action;

use super::sim::Simulation;

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

/// `part` as a percentage of `whole`, 0 when there is no whole.
pub(in crate::dev) fn pct(part: usize, whole: usize) -> f64 {
    if whole == 0 {
        return 0.0;
    }
    part as f64 * 100.0 / whole as f64
}

/// Escape a string for embedding in a JSON double-quoted value. No dependency: it
/// handles exactly what JSON requires — `"`, `\`, and control characters below 0x20.
pub(in crate::dev) fn json_escape(s: &str) -> String {
    let mut out = String::with_capacity(s.len() + 2);
    for c in s.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            c if (c as u32) < 0x20 => out.push_str(&format!("\\u{:04x}", c as u32)),
            c => out.push(c),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use funput_core::InputMethod;

    use super::super::sim::{SimConfig, simulate_with};
    use super::*;

    #[test]
    fn table_has_header_rows_and_summary() {
        let table = steps_table(&simulate_with(SimConfig::new(InputMethod::Telex), "as"));
        let lines: Vec<&str> = table.lines().collect();
        assert!(lines[0].starts_with("#"));
        assert_eq!(lines.len(), 1 + 2 + 1); // header + 2 steps + summary
        assert!(lines.last().unwrap().contains("á"));
    }
}
