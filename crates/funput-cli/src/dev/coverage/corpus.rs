//! Load a corpus file and filter it down to real Vietnamese syllables.

use std::collections::BTreeSet;
use std::fs;
use std::path::Path;

use funput_core::is_complete_syllable;

/// Malformed Viet74K entries — not valid Vietnamese, so excluded from the score.
/// Each has either two tone marks (a syllable carries exactly one) or a tone on the
/// wrong vowel; the engine correctly refuses to reproduce them, so they only depress
/// the number without exercising anything real. Kept explicit and auditable rather
/// than silently guessed at runtime.
const CORPUS_NOISE: &[&str] = &[
    // Two tone marks in one syllable.
    "chưởì",
    "cướì",
    "cưỡì",
    "dướỉ",
    "dịệp",
    "lèõ",
    "lưỡì",
    "lưỡí",
    "lưỡỉ",
    "lạị",
    "ngườì",
    "ngườí",
    "ngồí",
    "phổí",
    // Tone on the wrong vowel (engine produces the correctly-placed spelling).
    "ngươí",
    "ngóeo",
    "quoắt",
    "rià",
    "sóoc",
    "tơì",
    "tươì",
];

/// A token is a testable Vietnamese syllable when (a) it is not a known-malformed
/// corpus entry, (b) every letter is in the Vietnamese alphabet (`f`/`j`/`w`/`z` and
/// non-letters are not) and (c) it forms a structurally valid syllable. This excludes
/// acronyms (AIDS), chemical symbols (Ar/As), and foreign words (becgiê) so the score
/// reflects real Vietnamese.
fn is_vietnamese_syllable(token: &str) -> bool {
    !CORPUS_NOISE.contains(&token)
        && !token.is_empty()
        && token.chars().all(|c| {
            let l = c.to_lowercase().next().unwrap_or(c);
            l.is_alphabetic() && !matches!(l, 'f' | 'j' | 'w' | 'z')
        })
        && is_complete_syllable(token)
}

/// Read a corpus (one word per line; words may have multiple space-separated
/// syllables), split into unique Vietnamese syllables.
pub(super) fn load_syllables(path: &Path) -> std::io::Result<BTreeSet<String>> {
    let text = fs::read_to_string(path)?;
    let mut set = BTreeSet::new();
    for line in text.lines() {
        for token in line.split_whitespace() {
            let trimmed = token.trim_matches(|c: char| !c.is_alphabetic());
            if is_vietnamese_syllable(trimmed) {
                set.insert(trimmed.to_string());
            }
        }
    }
    Ok(set)
}
