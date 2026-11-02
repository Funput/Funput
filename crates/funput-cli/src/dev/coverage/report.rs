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

pub(super) use crate::dev::render::json_escape;
