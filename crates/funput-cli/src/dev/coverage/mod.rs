//! Round-trip coverage check: for each Vietnamese syllable in a corpus, encode
//! it to keystrokes, type it back through the engine, and compare.
//!
//! A syllable counts as correct if it reproduces under **either** tone style —
//! `hòa` and `hoà` are both valid spellings, so producing either is correct typing.
//! Smart-restore is off to measure pure composition.

mod corpus;
mod report;
#[cfg(test)]
mod tests;

use std::path::Path;

use funput_core::{InputMethod, ToneStyle};

use super::encode::encode;
use super::sim::{SimConfig, simulate_with};

/// Compose `keys` with smart-restore off (pure composition), traditional tone style —
/// used to show what the engine produced for a mismatch.
fn composed(keys: &str, method: InputMethod) -> String {
    let config = SimConfig {
        method,
        tone_style: ToneStyle::Traditional,
        smart_restore: false,
        spell_check: false,
    };
    simulate_with(config, keys).app_text
}

/// Does `syllable` reproduce when encoded and typed back under `method`?
/// Checks both tone styles.
fn round_trips(syllable: &str, encoding: InputMethod, method: InputMethod) -> bool {
    let keys = encode(syllable, encoding);
    [ToneStyle::Traditional, ToneStyle::Modern]
        .iter()
        .any(|&style| {
            let config = SimConfig {
                method,
                tone_style: style,
                smart_restore: false,
                spell_check: false,
            };
            simulate_with(config, &keys).app_text == syllable
        })
}

pub(super) struct MethodResult {
    label: &'static str,
    total: usize,
    covered: usize,
    mismatches: Vec<(String, String)>, // (syllable, produced under traditional)
}

fn evaluate(
    syllables: &[String],
    encoding: InputMethod,
    method: InputMethod,
    label: &'static str,
    keep_mismatches: usize,
) -> MethodResult {
    let mut covered = 0;
    let mut mismatches = Vec::new();
    for s in syllables {
        if round_trips(s, encoding, method) {
            covered += 1;
        } else if mismatches.len() < keep_mismatches {
            mismatches.push((s.clone(), composed(&encode(s, encoding), method)));
        }
    }
    MethodResult {
        label,
        total: syllables.len(),
        covered,
        mismatches,
    }
}

/// Run the round-trip coverage check and print a report.
pub fn run(
    corpus_path: &Path,
    json: bool,
    show_mismatches: usize,
    limit: Option<usize>,
) -> std::io::Result<()> {
    let mut syllables: Vec<String> = corpus::load_syllables(corpus_path)?.into_iter().collect();
    if let Some(n) = limit {
        syllables.truncate(n);
    }

    let results = [
        evaluate(
            &syllables,
            InputMethod::Telex,
            InputMethod::Telex,
            "Telex",
            show_mismatches,
        ),
        evaluate(
            &syllables,
            InputMethod::Telex,
            InputMethod::TelexAdvanced,
            "Advanced canonical",
            show_mismatches,
        ),
        evaluate(
            &syllables,
            InputMethod::TelexAdvanced,
            InputMethod::TelexAdvanced,
            "Advanced Full",
            show_mismatches,
        ),
        evaluate(
            &syllables,
            InputMethod::Vni,
            InputMethod::Vni,
            "VNI",
            show_mismatches,
        ),
    ];

    if json {
        report::print_json(corpus_path, &syllables, &results);
    } else {
        report::print_human(corpus_path, &syllables, &results, show_mismatches);
    }
    Ok(())
}
