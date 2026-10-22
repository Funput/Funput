//! English coverage results, for a person or as JSON.

use std::path::Path;

use super::Counts;
use super::outcome::Outcome;
use crate::dev::coverage::report::json_escape;

pub(super) fn print_human(lexicon: &Path, results: &[Counts]) {
    println!("Funput English coverage (lexicon: {})", lexicon.display());
    for counts in results {
        println!(
            "  {:<12} {} wrong of {}: {} lost a doubled key, {} left Vietnamese, {} other",
            counts.label,
            counts.wrong(),
            counts.total,
            counts.lost_key,
            counts.vietnamese,
            counts.other
        );
    }
    for counts in results.iter().filter(|counts| !counts.samples.is_empty()) {
        println!("\nSample wrong words ({}): typed → shown", counts.label);
        for (word, shown, outcome) in &counts.samples {
            let kind = match outcome {
                Outcome::LostKey => "lost a doubled key",
                Outcome::Vietnamese => "left Vietnamese",
                Outcome::Other | Outcome::Typed => "other",
            };
            println!("  {word} → {shown}  ({kind})");
        }
    }
}

pub(super) fn print_json(lexicon: &Path, results: &[Counts]) {
    let setups = results
        .iter()
        .map(|counts| {
            let fields = counts
                .entries()
                .iter()
                .map(|(name, count)| {
                    let field = name.rsplit('.').next().unwrap_or(name);
                    format!("\"{field}\":{count}")
                })
                .collect::<Vec<_>>()
                .join(",");
            format!("\"{}\":{{{fields}}}", counts.label.replace('.', "_"))
        })
        .collect::<Vec<_>>()
        .join(",");
    println!(
        "{{\"lexicon\":\"{}\",{setups}}}",
        json_escape(&lexicon.display().to_string())
    );
}
