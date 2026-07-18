//! Rendering of coverage results — human-readable report and machine-readable JSON.

use std::path::Path;

use super::MethodResult;

pub(super) fn print_human(
    corpus: &Path,
    syllables: &[String],
    results: &[MethodResult],
    show_mismatches: usize,
) {
    println!(
        "Funput coverage — {} syllables (corpus: {})",
        syllables.len(),
        corpus.display()
    );
    for r in results {
        println!(
            "  {:<6} {:.2}%  ({} wrong of {})",
            r.label,
            pct(r.covered, r.total),
            r.total - r.covered,
            r.total
        );
    }
    if show_mismatches > 0 {
        for r in results {
            if r.mismatches.is_empty() {
                continue;
            }
            println!("\nSample mismatches ({}): syllable → produced", r.label);
            for (syl, produced) in &r.mismatches {
                println!("  {syl} → {produced}");
            }
        }
    }
}

pub(super) fn print_json(corpus: &Path, syllables: &[String], results: &[MethodResult]) {
    let one = |r: &MethodResult| {
        format!(
            "{{\"total\":{},\"covered\":{},\"coverage\":{:.4}}}",
            r.total,
            r.covered,
            pct(r.covered, r.total) / 100.0
        )
    };
    let methods = results
        .iter()
        .map(|result| format!("\"{}\":{}", json_key(result.label), one(result)))
        .collect::<Vec<_>>()
        .join(",");
    println!(
        "{{\"corpus\":\"{}\",\"syllables\":{},{}}}",
        json_escape(&corpus.display().to_string()),
        syllables.len(),
        methods
    );
}

fn json_key(label: &str) -> String {
    label.to_ascii_lowercase().replace(' ', "_")
}

fn pct(covered: usize, total: usize) -> f64 {
    if total == 0 {
        100.0
    } else {
        covered as f64 / total as f64 * 100.0
    }
}

/// Escape a string for embedding in a JSON double-quoted value. No dependency: it
/// handles exactly what JSON requires — `"`, `\`, and control characters below 0x20.
fn json_escape(s: &str) -> String {
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
